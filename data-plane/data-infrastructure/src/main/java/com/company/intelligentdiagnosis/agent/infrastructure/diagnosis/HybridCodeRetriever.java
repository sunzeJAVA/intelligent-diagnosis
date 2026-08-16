package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeRetriever;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeSnippet;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.infrastructure.diagnosis.DiagnosisProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 混合代码检索器
 * <p>
 * 组合多种召回策略，提升检索覆盖率和精确度：
 * <ul>
 *   <li>向量召回（语义相似度）</li>
 *   <li>图召回（调用链路扩展）</li>
 * </ul>
 * <p>
 * 对多路结果进行去重，并按配置返回 TopK。
 * 该实现作为 {@link CodeRetriever} 的主 Bean 注入到应用服务中。
 */
@Primary
@Component
public class HybridCodeRetriever implements CodeRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridCodeRetriever.class);

    private final QdrantCodeRetriever vectorCodeRetriever;
    private final GraphCodeRetriever graphCodeRetriever;
    private final DiagnosisProperties diagnosisProperties;
    private final Counter retrievalFailureCounter;

    public HybridCodeRetriever(QdrantCodeRetriever vectorCodeRetriever,
                               GraphCodeRetriever graphCodeRetriever,
                               DiagnosisProperties diagnosisProperties,
                               MeterRegistry meterRegistry) {
        this.vectorCodeRetriever = vectorCodeRetriever;
        this.graphCodeRetriever = graphCodeRetriever;
        this.diagnosisProperties = diagnosisProperties;
        this.retrievalFailureCounter = Counter.builder("diagnosis.retrieval.failure.total")
            .description("Total number of code retrieval failures across all strategies")
            .tag("retriever", "hybrid")
            .register(meterRegistry);
    }

    @Override
    public List<CodeSnippet> retrieve(DiagnosisRequest request) {
        return retrieve(request, DiagnosisIntent.unknown(request.query() != null ? request.query() : ""));
    }

    @Override
    public List<CodeSnippet> retrieve(DiagnosisRequest request, DiagnosisIntent intent) {
        long start = System.currentTimeMillis();

        // 1. 向量召回
        List<CodeSnippet> vectorResults = safeRetrieve(vectorCodeRetriever, request, intent, "vector");

        // 2. 图召回
        List<CodeSnippet> graphResults = safeRetrieve(graphCodeRetriever, request, intent, "graph");

        // 3. 融合去重
        Set<SnippetKey> seen = new LinkedHashSet<>();
        List<CodeSnippet> merged = new ArrayList<>();

        // 优先保留向量结果（语义相关度高），再补充图召回结果
        for (CodeSnippet snippet : vectorResults) {
            if (seen.add(new SnippetKey(snippet.filePath(), snippet.startLine(), snippet.endLine()))) {
                merged.add(snippet);
            }
        }
        for (CodeSnippet snippet : graphResults) {
            if (seen.add(new SnippetKey(snippet.filePath(), snippet.startLine(), snippet.endLine()))) {
                merged.add(snippet);
            }
        }

        int topK = diagnosisProperties.getTopK();
        List<CodeSnippet> result = merged.size() > topK ? merged.subList(0, topK) : merged;

        log.info("Hybrid retrieval completed in {}ms: vector={}, graph={}, merged={}",
            System.currentTimeMillis() - start, vectorResults.size(), graphResults.size(), result.size());
        return result;
    }

    private List<CodeSnippet> safeRetrieve(CodeRetriever retriever, DiagnosisRequest request,
                                           DiagnosisIntent intent, String retrieverName) {
        try {
            return retriever.retrieve(request, intent);
        } catch (Exception e) {
            log.warn("{} retrieval failed: {}", retrieverName, e.getMessage());
            retrievalFailureCounter.increment();
            return List.of();
        }
    }

    /**
     * 用于去重的代码片段标识
     */
    private record SnippetKey(String filePath, int startLine, int endLine) {
    }
}
