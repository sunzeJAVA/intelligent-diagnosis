package com.company.intelligentdiagnosis.agent.infrastructure.enrichment;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.enrichment.CodeElementEnricher;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmCompletion;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 LLM 的代码元素摘要富化器
 * <p>
 * 在索引阶段为每个代码元素生成中英文摘要，提升后续诊断检索的语义召回质量。
 * 富化失败不会阻塞索引流程，而是跳过该元素的摘要生成。
 */
@Component
public class LlmCodeElementEnricher implements CodeElementEnricher {

    private static final Logger log = LoggerFactory.getLogger(LlmCodeElementEnricher.class);

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
    private static final Pattern BRACE_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");

    /**
     * 单次提交给 LLM 的源代码最大字符数，避免超出上下文窗口。
     */
    private static final int MAX_SOURCE_LENGTH = 4000;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final EnrichmentProperties properties;

    public LlmCodeElementEnricher(LlmClient llmClient,
                                  ObjectMapper objectMapper,
                                  EnrichmentProperties properties) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public List<CodeElement> enrich(List<CodeElement> elements) {
        if (!properties.isEnabled() || elements == null || elements.isEmpty()) {
            return elements;
        }

        List<CodeElement> enriched = new ArrayList<>(elements.size());
        for (CodeElement element : elements) {
            try {
                enriched.add(enrichSingle(element));
            } catch (Exception e) {
                log.warn("Failed to enrich element {}, skipping: {}", element.id(), e.getMessage());
                enriched.add(element);
            }
        }
        return enriched;
    }

    private CodeElement enrichSingle(CodeElement element) {
        if (shouldSkip(element)) {
            return element;
        }

        LlmCompletion completion = llmClient.complete(buildSystemPrompt(), buildUserPrompt(element));
        if (completion.degraded()) {
            log.warn("LLM returned degraded response for element {}, skipping enrichment", element.id());
            return element;
        }

        SummaryResponse response = parseResponse(completion.content());
        if (response == null) {
            return element;
        }

        return element.withSummaries(
            nullToEmpty(response.chineseSummary()),
            nullToEmpty(response.englishSummary())
        );
    }

    private boolean shouldSkip(CodeElement element) {
        return element == null || element.id() == null;
    }

    private String buildSystemPrompt() {
        return """
            你是代码摘要生成助手。请根据给出的代码元素信息，生成简洁的中英文摘要。

            输出格式（严格 JSON，不要添加任何额外文本）：
            {
              "chineseSummary": "用1-2句中文描述该代码元素的作用，例如：获取用户详情信息，包含权限校验和缓存读取逻辑。",
              "englishSummary": "用1-2句英文描述该代码元素的作用，例如：Retrieves user details including permission check and cache read logic."
            }

            要求：
            1. 摘要应突出该元素的职责、输入/输出和关键业务含义
            2. 如果代码包含空指针、并发、数据库、性能等潜在风险点，可在摘要中提及
            3. 仅输出 JSON，不要包含 Markdown 代码块或其他说明
            """;
    }

    private String buildUserPrompt(CodeElement element) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下代码元素生成中英文摘要：\n\n");
        sb.append("类型：").append(element.kind()).append("\n");
        if (element.qualifiedName() != null && !element.qualifiedName().isBlank()) {
            sb.append("全限定名：").append(element.qualifiedName()).append("\n");
        }
        if (element.documentation() != null && !element.documentation().isBlank()) {
            sb.append("文档注释：").append(element.documentation()).append("\n");
        }
        sb.append("源代码：\n").append(truncate(element.sourceCode()));
        return sb.toString();
    }

    private String truncate(String sourceCode) {
        if (sourceCode == null) {
            return "";
        }
        if (sourceCode.length() <= MAX_SOURCE_LENGTH) {
            return sourceCode;
        }
        return sourceCode.substring(0, MAX_SOURCE_LENGTH) + "\n... (truncated)";
    }

    private SummaryResponse parseResponse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String json = extractJson(raw);
        if (json == null) {
            log.warn("Failed to extract JSON from LLM summary response. Raw: {}", raw);
            return null;
        }
        try {
            return objectMapper.readValue(json, SummaryResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse summary JSON: {}", e.getMessage());
            return null;
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

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record SummaryResponse(
        @JsonProperty("chineseSummary") String chineseSummary,
        @JsonProperty("englishSummary") String englishSummary
    ) {
    }
}
