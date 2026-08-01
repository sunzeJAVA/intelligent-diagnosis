package com.company.intelligentdiagnosis.agent.infrastructure.graph;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.Relation;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Neo4j 图存储客户端
 * 负责代码元素关系图的构建和管理
 */
@Component
public class GraphStoreClient {

    private static final Logger log = LoggerFactory.getLogger(GraphStoreClient.class);

    private final Driver driver;

    /**
     * 构造函数
     *
     * @param driver Neo4j 驱动
     */
    public GraphStoreClient(Driver driver) {
        this.driver = driver;
    }

    /**
     * 构建代码关系图到生产标签
     */
    public void buildGraph(String repository, String commitHash, List<CodeElement> elements) {
        buildGraph(repository, commitHash, elements, false);
    }

    /**
     * 构建代码关系图到沙箱标签
     */
    public void buildSandboxGraph(String repository, String commitHash, List<CodeElement> elements) {
        buildGraph(repository, commitHash, elements, true);
    }

    private void buildGraph(String repository, String commitHash, List<CodeElement> elements, boolean sandbox) {
        if (elements.isEmpty()) {
            return;
        }

        String nodeLabel = sandbox ? "CodeElementSandbox" : "CodeElement";
        String relType = sandbox ? "SANDBOX_RELATES_TO" : "RELATES_TO";

        List<Map<String, Object>> nodeParams = elements.stream()
            .map(element -> toNodeParameters(repository, commitHash, element))
            .toList();

        List<Map<String, Object>> relationParams = elements.stream()
            .flatMap(element -> element.relations().stream()
                .map(relation -> toRelationParameters(repository, element.id(), relation)))
            .toList();

        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> {
                tx.run("""
                    UNWIND $nodes AS node
                    MERGE (e:%s {id: node.id})
                    SET e.repository = node.repository,
                        e.commitHash = node.commitHash,
                        e.kind = node.kind,
                        e.name = node.name,
                        e.qualifiedName = node.qualifiedName,
                        e.filePath = node.filePath,
                        e.startLine = node.startLine,
                        e.endLine = node.endLine,
                        e.sourceCode = node.sourceCode,
                        e.documentation = node.documentation,
                        e.modifiers = node.modifiers
                    """.formatted(nodeLabel), Values.parameters("nodes", nodeParams));

                if (!relationParams.isEmpty()) {
                    tx.run("""
                        UNWIND $relations AS rel
                        MATCH (source:%s {id: rel.sourceId})
                        MATCH (target:%s {id: rel.targetId})
                        MERGE (source)-[r:%s]->(target)
                        SET r.kind = rel.kind, r.repository = rel.repository
                        """.formatted(nodeLabel, nodeLabel, relType), Values.parameters("relations", relationParams));
                }
            });
        }

        log.info("Built Neo4j {} graph for repository {} ({} elements, {} relations)",
            sandbox ? "sandbox" : "production", repository, elements.size(), relationParams.size());
    }

    /**
     * 将沙箱图提升到生产图
     */
    public void promoteSandboxToProduction(String repository) {
        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> {
                tx.run("""
                    MATCH (s:CodeElementSandbox {repository: $repository})
                    MERGE (p:CodeElement {id: s.id})
                    SET p = properties(s)
                    REMOVE p:CodeElementSandbox
                    """, Values.parameters("repository", repository));

                tx.run("""
                    MATCH (:CodeElementSandbox {repository: $repository})-[sr:SANDBOX_RELATES_TO]->(:CodeElementSandbox {repository: $repository})
                    WITH sr, startNode(sr) AS src, endNode(sr) AS tgt
                    MATCH (pSrc:CodeElement {id: src.id}), (pTgt:CodeElement {id: tgt.id})
                    MERGE (pSrc)-[r:RELATES_TO]->(pTgt)
                    SET r.kind = sr.kind, r.repository = sr.repository
                    """, Values.parameters("repository", repository));

                tx.run("""
                    MATCH (s:CodeElementSandbox {repository: $repository})
                    OPTIONAL MATCH (s)-[sr:SANDBOX_RELATES_TO]-()
                    DELETE sr, s
                    """, Values.parameters("repository", repository));
            });
        }
        log.info("Promoted Neo4j sandbox graph to production for repository {}", repository);
    }

    /**
     * 删除指定仓库的生产图数据
     */
    public void deleteByRepository(String repository) {
        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run("""
                MATCH (e:CodeElement {repository: $repository})
                OPTIONAL MATCH (e)-[r]-()
                DELETE r, e
                """, Values.parameters("repository", repository)));
        }
        log.info("Deleted Neo4j graph for repository {}", repository);
    }

    /**
     * 删除指定仓库的沙箱图数据
     */
    public void deleteSandboxByRepository(String repository) {
        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> tx.run("""
                MATCH (e:CodeElementSandbox {repository: $repository})
                OPTIONAL MATCH (e)-[r]-()
                DELETE r, e
                """, Values.parameters("repository", repository)));
        }
        log.info("Deleted Neo4j sandbox graph for repository {}", repository);
    }

    /**
     * 统计生产图中所有节点数
     */
    public long countAllNodes() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run("""
                MATCH (e:CodeElement)
                RETURN count(e) AS count
                """).single().get("count").asLong());
        }
    }

    /**
     * 统计生产图中所有关系数
     */
    public long countAllRelations() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run("""
                MATCH ()-[r:RELATES_TO]->()
                RETURN count(r) AS count
                """).single().get("count").asLong());
        }
    }

    /**
     * 统计生产图中指定仓库的节点数
     */
    public long countNodes(String repository) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run("""
                MATCH (e:CodeElement {repository: $repository})
                RETURN count(e) AS count
                """, Values.parameters("repository", repository))
                .single().get("count").asLong());
        }
    }

    /**
     * 统计生产图中指定仓库的关系数
     */
    public long countRelations(String repository) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run("""
                MATCH ()-[r:RELATES_TO]->()
                WHERE r.repository = $repository
                RETURN count(r) AS count
                """, Values.parameters("repository", repository))
                .single().get("count").asLong());
        }
    }

    /**
     * 将代码元素转换为节点参数
     *
     * @param repository  仓库名称
     * @param commitHash  提交哈希
     * @param element     代码元素
     * @return 节点参数映射
     */
    private Map<String, Object> toNodeParameters(String repository, String commitHash, CodeElement element) {
        return Map.ofEntries(
            Map.entry("id", element.id()),
            Map.entry("repository", repository),
            Map.entry("commitHash", commitHash),
            Map.entry("kind", element.kind().name()),
            Map.entry("name", element.name()),
            Map.entry("qualifiedName", nullToEmpty(element.qualifiedName())),
            Map.entry("filePath", nullToEmpty(element.filePath())),
            Map.entry("startLine", element.startLine()),
            Map.entry("endLine", element.endLine()),
            Map.entry("sourceCode", nullToEmpty(element.sourceCode())),
            Map.entry("documentation", nullToEmpty(element.documentation())),
            Map.entry("modifiers", element.modifiers() != null ? element.modifiers() : List.of())
        );
    }

    /**
     * 将关系转换为关系参数
     *
     * @param sourceId 源元素 ID
     * @param relation 关系
     * @return 关系参数映射
     */
    private Map<String, Object> toRelationParameters(String repository, String sourceId, Relation relation) {
        return Map.of(
            "repository", repository,
            "sourceId", sourceId,
            "targetId", relation.targetId(),
            "kind", relation.kind().name()
        );
    }

    /**
     * 空字符串处理
     *
     * @param value 字符串值
     * @return 非空字符串
     */
    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
