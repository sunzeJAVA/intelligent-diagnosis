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

@Component
public class GraphStoreClient {

    private static final Logger log = LoggerFactory.getLogger(GraphStoreClient.class);

    private final Driver driver;

    public GraphStoreClient(Driver driver) {
        this.driver = driver;
    }

    public void buildGraph(String repository, String commitHash, List<CodeElement> elements) {
        if (elements.isEmpty()) {
            return;
        }

        List<Map<String, Object>> nodeParams = elements.stream()
            .map(element -> toNodeParameters(repository, commitHash, element))
            .toList();

        List<Map<String, Object>> relationParams = elements.stream()
            .flatMap(element -> element.relations().stream()
                .map(relation -> toRelationParameters(element.id(), relation)))
            .toList();

        try (Session session = driver.session()) {
            session.executeWriteWithoutResult(tx -> {
                tx.run("""
                    UNWIND $nodes AS node
                    MERGE (e:CodeElement {id: node.id})
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
                    """, Values.parameters("nodes", nodeParams));

                if (!relationParams.isEmpty()) {
                    tx.run("""
                        UNWIND $relations AS rel
                        MATCH (source:CodeElement {id: rel.sourceId})
                        MATCH (target:CodeElement {id: rel.targetId})
                        MERGE (source)-[r:RELATES_TO]->(target)
                        SET r.kind = rel.kind
                        """, Values.parameters("relations", relationParams));
                }
            });
        }

        log.info("Built Neo4j graph for repository {} ({} elements, {} relations)",
            repository, elements.size(), relationParams.size());
    }

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

    private Map<String, Object> toRelationParameters(String sourceId, Relation relation) {
        return Map.of(
            "sourceId", sourceId,
            "targetId", relation.targetId(),
            "kind", relation.kind().name()
        );
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
