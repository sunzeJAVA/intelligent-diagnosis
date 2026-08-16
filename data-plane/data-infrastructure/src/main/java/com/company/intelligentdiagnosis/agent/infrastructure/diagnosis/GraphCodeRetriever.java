package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeRetriever;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeSnippet;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.infrastructure.graph.GraphCodeElement;
import com.company.intelligentdiagnosis.agent.infrastructure.graph.GraphStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 Neo4j 图数据库的代码检索器
 * <p>
 * 利用代码元素之间的调用关系（CALLS）进行召回：
 * <ul>
 *   <li>根据意图识别出的关键实体（类名、方法名）在图中定位节点</li>
 *   <li>沿 CALLS 关系扩展 1-2 跳，召回上下游调用方法</li>
 *   <li>适合定位"接口 → Service → Mapper"这类完整调用链路</li>
 * </ul>
 */
@Component
public class GraphCodeRetriever implements CodeRetriever {

    private static final Logger log = LoggerFactory.getLogger(GraphCodeRetriever.class);

    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b([A-Z][a-zA-Z0-9]*(?:Exception|Error)?)\\b");
    private static final int CALL_CHAIN_DEPTH = 2;

    private final GraphStoreClient graphStoreClient;

    public GraphCodeRetriever(GraphStoreClient graphStoreClient) {
        this.graphStoreClient = graphStoreClient;
    }

    @Override
    public List<CodeSnippet> retrieve(DiagnosisRequest request) {
        return retrieve(request, DiagnosisIntent.unknown(request.query() != null ? request.query() : ""));
    }

    @Override
    public List<CodeSnippet> retrieve(DiagnosisRequest request, DiagnosisIntent intent) {
        String service = request.service();
        if (service == null || service.isBlank()) {
            return List.of();
        }

        Set<String> entityNames = collectEntityNames(request, intent);
        if (entityNames.isEmpty()) {
            log.debug("No entities extracted for graph retrieval, service={}", service);
            return List.of();
        }

        log.info("Graph retrieval for service {}: entities={}", service, entityNames);
        Set<CodeSnippet> results = new LinkedHashSet<>();

        for (String entity : entityNames) {
            if (entity.length() < 2) {
                continue;
            }
            List<GraphCodeElement> matchedNodes = graphStoreClient.findNodesByName(service, entity);
            log.debug("Entity '{}' matched {} nodes in graph", entity, matchedNodes.size());

            for (GraphCodeElement node : matchedNodes) {
                // 加入节点本身
                results.add(toCodeSnippet(node));

                // 沿调用链扩展
                List<GraphCodeElement> chain = graphStoreClient.expandCallChain(service, node.id(), CALL_CHAIN_DEPTH);
                for (GraphCodeElement chainNode : chain) {
                    results.add(toCodeSnippet(chainNode));
                }
            }
        }

        return new ArrayList<>(results);
    }

    /**
     * 收集用于图检索的候选实体名
     * 来源：意图识别出的 entities + 从 query 中 regex 提取的类名
     */
    private Set<String> collectEntityNames(DiagnosisRequest request, DiagnosisIntent intent) {
        Set<String> names = new LinkedHashSet<>();

        if (intent.entities() != null) {
            for (String entity : intent.entities()) {
                if (entity != null && !entity.isBlank()) {
                    names.add(entity);
                }
            }
        }

        String query = request.query();
        if (query != null && !query.isBlank()) {
            Matcher matcher = CLASS_PATTERN.matcher(query);
            while (matcher.find()) {
                String match = matcher.group(1);
                if (match.length() > 2) {
                    names.add(match);
                }
            }
        }

        return names;
    }

    private CodeSnippet toCodeSnippet(GraphCodeElement node) {
        String content = node.sourceCode() != null && !node.sourceCode().isBlank()
            ? node.sourceCode()
            : "// " + node.qualifiedName();
        return new CodeSnippet(node.filePath(), node.startLine(), node.endLine(), content);
    }
}
