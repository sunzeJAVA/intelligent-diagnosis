package com.company.intelligentdiagnosis.agent.infrastructure.backup;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Neo4j 图索引物理备份客户端
 * <p>
 * 优先尝试使用 APOC 的 <code>apoc.export.cypher.query</code> 以流式方式导出 Cypher 语句；
 * 当 Neo4j 未安装 APOC 插件时，自动降级为手动生成 MERGE 语句，保证备份/恢复能力可用。
 */
@Component
public class Neo4jBackupClient {

    private static final Logger log = LoggerFactory.getLogger(Neo4jBackupClient.class);
    private static final String BACKUP_FILE = "neo4j-backup.cypher";
    private static final String APOC_VERSION_QUERY = "RETURN apoc.version() AS version";

    private final Driver driver;
    private final BackupStorage backupStorage;

    public Neo4jBackupClient(Driver driver, BackupStorage backupStorage) {
        this.driver = driver;
        this.backupStorage = backupStorage;
    }

    /**
     * 为指定仓库创建 Neo4j 图备份，返回本地备份文件绝对路径
     */
    public String createBackup(String repositoryName, String snapshotId) {
        boolean apocAvailable = isApocAvailable();
        String cypher;
        if (apocAvailable) {
            cypher = exportWithApoc(repositoryName);
            log.info("Created Neo4j backup using APOC for repository {} snapshot {}", repositoryName, snapshotId);
        } else {
            cypher = exportManually(repositoryName);
            log.info("Created Neo4j backup using manual Cypher export for repository {} snapshot {}", repositoryName, snapshotId);
        }

        Path path = backupStorage.writeString(repositoryName, snapshotId, BACKUP_FILE, cypher);
        return path.toAbsolutePath().toString();
    }

    /**
     * 从本地 Cypher 备份文件恢复指定仓库的图数据
     */
    public void restoreBackup(String repositoryName, String snapshotId) {
        Path file = backupStorage.resolveSnapshotDir(repositoryName, snapshotId).resolve(BACKUP_FILE);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Neo4j backup file not found: " + file);
        }

        List<String> statements;
        try {
            String content = Files.readString(file);
            statements = splitStatements(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Neo4j backup: " + file, e);
        }

        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> {
                for (String statement : statements) {
                    if (!statement.isBlank()) {
                        tx.run(statement);
                    }
                }
            });
        }
        log.info("Restored Neo4j graph for repository {} snapshot {} ({} statements)",
            repositoryName, snapshotId, statements.size());
    }

    private boolean isApocAvailable() {
        try (Session session = driver.session()) {
            session.run(APOC_VERSION_QUERY).consume();
            return true;
        } catch (Exception e) {
            log.debug("APOC plugin not available: {}", e.getMessage());
            return false;
        }
    }

    private String exportWithApoc(String repositoryName) {
        String query = """
            MATCH (e:CodeElement {repository: $repo})
            OPTIONAL MATCH (e)-[r:RELATES_TO]->()
            RETURN e, r
            """;
        String apocCall = """
            CALL apoc.export.cypher.query($query, null, {stream: true, format: 'cypher', params: {repo: $repo}})
            YIELD data
            RETURN data
            """;

        try (Session session = driver.session()) {
            Result result = session.run(apocCall,
                Values.parameters("query", query, "repo", repositoryName));
            StringBuilder builder = new StringBuilder();
            while (result.hasNext()) {
                Record record = result.next();
                builder.append(record.get("data").asString());
            }
            return builder.toString();
        }
    }

    private String exportManually(String repositoryName) {
        StringBuilder builder = new StringBuilder();

        // 备份节点
        try (Session session = driver.session()) {
            Result result = session.run("""
                MATCH (e:CodeElement {repository: $repository})
                RETURN e.id AS id, properties(e) AS props
                """, Values.parameters("repository", repositoryName));

            while (result.hasNext()) {
                Record record = result.next();
                String id = record.get("id").asString();
                Map<String, Object> props = record.get("props").asMap();
                builder.append(buildNodeMerge(id, props)).append(";\n");
            }
        }

        // 备份关系
        try (Session session = driver.session()) {
            Result result = session.run("""
                MATCH (source:CodeElement {repository: $repository})-[r:RELATES_TO]->(target:CodeElement {repository: $repository})
                RETURN source.id AS sourceId, target.id AS targetId, properties(r) AS props
                """, Values.parameters("repository", repositoryName));

            while (result.hasNext()) {
                Record record = result.next();
                String sourceId = record.get("sourceId").asString();
                String targetId = record.get("targetId").asString();
                Map<String, Object> props = record.get("props").asMap();
                builder.append(buildRelationshipMerge(sourceId, targetId, props)).append(";\n");
            }
        }

        return builder.toString();
    }

    private String buildNodeMerge(String id, Map<String, Object> props) {
        StringBuilder setClause = new StringBuilder();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (setClause.length() > 0) {
                setClause.append(", ");
            }
            setClause.append("n.").append(entry.getKey()).append(" = ").append(formatValue(entry.getValue()));
        }
        return "MERGE (n:CodeElement {id: " + formatValue(id) + "}) SET " + setClause;
    }

    private String buildRelationshipMerge(String sourceId, String targetId, Map<String, Object> props) {
        StringBuilder setClause = new StringBuilder();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (setClause.length() > 0) {
                setClause.append(", ");
            }
            setClause.append("r.").append(entry.getKey()).append(" = ").append(formatValue(entry.getValue()));
        }
        String match = "MATCH (a:CodeElement {id: " + formatValue(sourceId)
            + "}), (b:CodeElement {id: " + formatValue(targetId) + "}) ";
        String merge = "MERGE (a)-[r:RELATES_TO]->(b)";
        String set = setClause.isEmpty() ? "" : " SET " + setClause;
        return match + merge + set;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return "'" + s.replace("'", "\\'") + "'";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof List<?> list) {
            return "[" + list.stream().map(this::formatValue).reduce((a, b) -> a + ", " + b).orElse("") + "]";
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> list = new java.util.ArrayList<>();
            iterable.forEach(list::add);
            return formatValue(list);
        }
        return "'" + value.toString().replace("'", "\\'") + "'";
    }

    private List<String> splitStatements(String content) {
        // 简单按分号拆分；APOC 生成的脚本以及手动脚本均以此为语句分隔符
        return List.of(content.split(";\\s*\\n")).stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    }
}
