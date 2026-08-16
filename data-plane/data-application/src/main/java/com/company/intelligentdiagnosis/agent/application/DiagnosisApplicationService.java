package com.company.intelligentdiagnosis.agent.application;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeRetriever;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeSnippet;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisAuditor;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisResponse;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.IntentRecognizer;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.PolicyEngine;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.QueryRewriter;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.RewrittenQuery;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 诊断应用服务
 * 协调诊断流程：策略验证 → 代码检索 → LLM分析 → 结果解析 → 审计记录
 */
@Service
public class DiagnosisApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisApplicationService.class);

    private static final String SYSTEM_PROMPT = """
        You are an expert software engineering assistant. Given a user query, error information, service name, and relevant code snippets, analyze the problem and produce a structured diagnosis in Chinese.
        Return ONLY a JSON object with this shape:
        {
          "summary": "one-sentence summary of the issue",
          "rootCause": "detailed root cause analysis",
          "suggestions": ["actionable fix 1", "actionable fix 2", ...]
        }
        Do not include markdown fences, explanations, or any text outside the JSON.
        """;

    private final CodeRetriever codeRetriever;
    private final LlmClient llmClient;
    private final DiagnosisAuditor auditor;
    private final PolicyEngine policyEngine;
    private final IntentRecognizer intentRecognizer;
    private final QueryRewriter queryRewriter;
    private final ObjectMapper objectMapper;
    private final Counter diagnosisCounter;

    /**
     * 构造函数
     *
     * @param codeRetriever    代码检索器
     * @param llmClient        LLM 客户端
     * @param auditor          诊断审计器
     * @param policyEngine     策略引擎
     * @param intentRecognizer 意图识别器
     * @param queryRewriter    Query 重写器
     * @param objectMapper     JSON 序列化器
     */
    public DiagnosisApplicationService(CodeRetriever codeRetriever,
                                       LlmClient llmClient,
                                       DiagnosisAuditor auditor,
                                       PolicyEngine policyEngine,
                                       IntentRecognizer intentRecognizer,
                                       QueryRewriter queryRewriter,
                                       ObjectMapper objectMapper,
                                       Counter diagnosisCounter) {
        this.codeRetriever = codeRetriever;
        this.llmClient = llmClient;
        this.auditor = auditor;
        this.policyEngine = policyEngine;
        this.intentRecognizer = intentRecognizer;
        this.queryRewriter = queryRewriter;
        this.objectMapper = objectMapper;
        this.diagnosisCounter = diagnosisCounter;
    }

    /**
     * 执行智能诊断
     * <p>
     * 流程：策略验证 → 意图识别 → 代码检索（增强 query）→ LLM 分析（增强 prompt）→ 结果解析 → 审计记录
     *
     * @param request 诊断请求
     * @return 诊断响应
     */
    @Observed(name = "diagnosis.execute", contextualName = "diagnose", lowCardinalityKeyValues = {"flow", "diagnosis"})
    public DiagnosisResponse diagnose(DiagnosisRequest request) {
        long start = System.currentTimeMillis();
        policyEngine.validate(request);

        // 意图识别：分类问题类型 + 提取关键实体 + 增强检索 query
        DiagnosisIntent intent = intentRecognizer.recognize(request);
        log.info("Intent recognized for service {}: type={}, confidence={}", request.service(), intent.type(), intent.confidence());

        // Query 重写：生成面向检索的 searchQuery 和面向 LLM prompt 的 llmPromptQuery
        RewrittenQuery rewrittenQuery = queryRewriter.rewrite(request.query(), intent);
        DiagnosisRequest searchRequest = withSearchQuery(request, rewrittenQuery);
        List<CodeSnippet> relatedCode = codeRetriever.retrieve(searchRequest, intent);

        // 构建包含意图上下文的 LLM prompt
        String userPrompt = buildUserPrompt(request, intent, rewrittenQuery, relatedCode);
        var llmResult = llmClient.complete(SYSTEM_PROMPT, userPrompt);
        DiagnosisResponse response = parseResponse(llmResult.content(), relatedCode, intent, llmResult.degraded());

        long duration = System.currentTimeMillis() - start;
        auditor.record(request, response, duration);
        if (!response.degraded()) {
            diagnosisCounter.increment();
        } else {
            log.warn("Diagnosis returned degraded response for service {}, not incrementing success counter", request.service());
        }

        return response;
    }

    /**
     * 用重写后的 searchQuery 替换请求中的 query 字段，用于增强向量检索
     * 保留原始 errorInfo 和 service
     */
    private DiagnosisRequest withSearchQuery(DiagnosisRequest original, RewrittenQuery rewrittenQuery) {
        String searchQuery = rewrittenQuery.searchQuery();
        if (searchQuery == null || searchQuery.isBlank() || searchQuery.equals(original.query())) {
            return original;
        }
        return new DiagnosisRequest(searchQuery, original.errorInfo(), original.service(), original.userId(), original.tenantId());
    }

    /**
     * 构建用户提示词
     * 将诊断请求、意图识别结果、重写后的 query 和相关代码片段组合成完整的提示词
     *
     * @param request        原始诊断请求
     * @param intent         意图识别结果
     * @param rewrittenQuery Query 重写结果
     * @param snippets       相关代码片段列表
     * @return 完整的用户提示词
     */
    private String buildUserPrompt(DiagnosisRequest request, DiagnosisIntent intent, RewrittenQuery rewrittenQuery, List<CodeSnippet> snippets) {
        StringBuilder builder = new StringBuilder();
        builder.append("Service: ").append(request.service()).append("\n");
        // 注入意图识别上下文，帮助 LLM 聚焦问题类型
        builder.append("Problem Category: ").append(intent.type().getDisplayName())
            .append(" (confidence: ").append(String.format("%.2f", intent.confidence())).append(")\n");
        if (!intent.entities().isEmpty()) {
            builder.append("Key Entities: ").append(String.join(", ", intent.entities())).append("\n");
        }
        String promptQuery = rewrittenQuery.llmPromptQuery();
        if (promptQuery != null && !promptQuery.isBlank()) {
            builder.append("Query: ").append(promptQuery).append("\n");
        } else if (request.query() != null && !request.query().isBlank()) {
            builder.append("Query: ").append(request.query()).append("\n");
        }
        if (request.errorInfo() != null && !request.errorInfo().isBlank()) {
            builder.append("Error Info: ").append(request.errorInfo()).append("\n");
        }
        if (!snippets.isEmpty()) {
            builder.append("\nRelevant Code Snippets:\n");
            for (CodeSnippet snippet : snippets) {
                builder.append(snippet.filePath())
                    .append(":").append(snippet.startLine()).append("-").append(snippet.endLine())
                    .append("\n```\n")
                    .append(snippet.content())
                    .append("\n```\n\n");
            }
        }
        return builder.toString().trim();
    }

    /**
     * 解析 LLM 响应
     * 将 LLM 返回的 JSON 字符串解析为 DiagnosisResponse 对象
     *
     * @param rawResponse LLM 原始响应
     * @param relatedCode 相关代码片段列表
     * @param intent      意图识别结果
     * @return 解析后的诊断响应
     */
    private DiagnosisResponse parseResponse(String rawResponse,
                                            List<CodeSnippet> relatedCode,
                                            DiagnosisIntent intent,
                                            boolean llmDegraded) {
        String cleaned = stripMarkdownFences(rawResponse);
        try {
            ParsedLlmResponse parsed = objectMapper.readValue(cleaned, ParsedLlmResponse.class);
            return new DiagnosisResponse(
                parsed.summary,
                parsed.rootCause,
                parsed.suggestions,
                relatedCode,
                intent,
                llmDegraded
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LLM response as JSON, using fallback. Response: {}", rawResponse, e);
            return new DiagnosisResponse(
                rawResponse.trim(),
                "",
                List.of(),
                relatedCode,
                intent,
                true
            );
        }
    }

    /**
     * 移除 Markdown 代码块围栏
     *
     * @param text 原始文本
     * @return 移除围栏后的文本
     */
    private String stripMarkdownFences(String text) {
        return text.replaceAll("(?s)^\\s*```(?:json)?\\s*", "")
            .replaceAll("(?s)\\s*```\\s*$", "");
    }

    /**
     * LLM 响应解析结构
     */
    private record ParsedLlmResponse(
        String summary,
        String rootCause,
        List<String> suggestions
    ) {
    }
}
