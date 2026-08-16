package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.IntentType;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.QueryRewriter;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.RewrittenQuery;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于 LLM 的 Query 重写器
 * <p>
 * 利用 LLM 对原始 query 进行语义增强，输出：
 * <ul>
 *   <li>{@code searchQuery}：面向向量检索，补充同义词、技术栈关键词、英文表达</li>
 *   <li>{@code llmPromptQuery}：面向 LLM 诊断，保留完整语义并突出关键实体</li>
 * </ul>
 * <p>
 * 本实现只负责调用 LLM 并解析响应；LLM 失败时的降级策略由外层（如 {@link CompositeQueryRewriter}）决定，
 * 这样可以保持职责单一，也方便单独对 LLM 重写做单元测试。
 */
@Component
public class LlmQueryRewriter implements QueryRewriter {

    private static final Logger log = LoggerFactory.getLogger(LlmQueryRewriter.class);

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
    private static final Pattern BRACE_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");

    private static final String SYSTEM_PROMPT = """
        你是代码诊断系统的 Query 重写专家。根据用户原始问题描述和意图识别结果，生成两段查询文本：
        1. searchQuery：用于向量检索，需要补充同义词、技术栈关键词、英文表达，提升代码召回率。
        2. llmPromptQuery：用于给 LLM 做诊断的 prompt，保留完整语义并突出关键实体和分类。

        输出格式（严格 JSON，不要添加任何额外文本）：
        {
          "searchQuery": "用于向量检索的增强关键词",
          "llmPromptQuery": "用于 LLM 诊断 prompt 的查询文本"
        }

        要求：
        - searchQuery 用英文/技术术语为主，多补同义词（如 NPE 要展开成 NullPointerException）
        - 如果用户输入是中文技术术语，要在 searchQuery 中补充对应的英文术语（如 空指针 → NullPointerException，数据库 → SQL database，死锁 → deadlock）
        - llmPromptQuery 用自然语言，保留原始问题语义，可混合中英文
        - 如果原始 query 为空，则以 errorInfo 和意图实体为基础生成
        """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public LlmQueryRewriter(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public RewrittenQuery rewrite(String originalQuery, DiagnosisIntent intent) {
        if (originalQuery == null || originalQuery.isBlank()) {
            return RewrittenQuery.identity("");
        }

        String userPrompt = buildUserPrompt(originalQuery, intent);
        String raw = llmClient.complete(SYSTEM_PROMPT, userPrompt).content();
        return parseResponse(raw, originalQuery);
    }

    private String buildUserPrompt(String originalQuery, DiagnosisIntent intent) {
        String intentList = java.util.Arrays.stream(IntentType.values())
            .map(t -> "- " + t.name() + ": " + t.getDescription())
            .collect(Collectors.joining("\n"));

        return """
            原始问题描述：
            %s

            意图识别结果：
            - 类型：%s
            - 置信度：%.2f
            - 关键实体：%s

            可选意图类型：
            %s

            请输出 JSON 格式的重写结果。
            """.formatted(
                originalQuery,
                intent.type().getDisplayName(),
                intent.confidence(),
                intent.entities() != null ? String.join(", ", intent.entities()) : "",
                intentList
            );
    }

    private RewrittenQuery parseResponse(String raw, String originalQuery) {
        if (raw == null || raw.isBlank()) {
            log.warn("Empty LLM response for query rewrite");
            throw new QueryRewriteException("Empty LLM response for query rewrite");
        }

        String json = extractJson(raw);
        if (json == null) {
            log.warn("Failed to extract JSON from query rewrite response. Raw: {}", raw);
            throw new QueryRewriteException("Failed to extract JSON from query rewrite response");
        }

        try {
            RewriteResponse resp = objectMapper.readValue(json, RewriteResponse.class);
            String searchQuery = isBlank(resp.searchQuery()) ? originalQuery : resp.searchQuery().trim();
            String llmPromptQuery = isBlank(resp.llmPromptQuery()) ? originalQuery : resp.llmPromptQuery().trim();

            log.info("LLM query rewritten: searchQuery='{}', llmPromptQuery='{}'", searchQuery, llmPromptQuery);
            return new RewrittenQuery(searchQuery, llmPromptQuery);
        } catch (Exception e) {
            log.warn("Failed to parse query rewrite JSON: {}", e.getMessage());
            throw new QueryRewriteException("Failed to parse query rewrite JSON", e);
        }
    }

    private String extractJson(String raw) {
        Matcher codeBlock = CODE_BLOCK_PATTERN.matcher(raw);
        if (codeBlock.find()) {
            return codeBlock.group(1).trim();
        }
        Matcher brace = BRACE_PATTERN.matcher(raw);
        if (brace.find()) {
            return brace.group();
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RewriteResponse(
        @JsonProperty("searchQuery") String searchQuery,
        @JsonProperty("llmPromptQuery") String llmPromptQuery
    ) {
    }
}
