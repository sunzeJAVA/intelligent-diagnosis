package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.IntentRecognizer;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.IntentType;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于 LLM 的意图识别器
 * <p>
 * 调用 LLM 对用户输入的错误信息/问题描述进行：
 * <ul>
 *   <li>意图分类（{@link IntentType}）</li>
 *   <li>置信度评估（0.0-1.0）</li>
 *   <li>关键实体提取（类名、方法名、异常类型等）</li>
 *   <li>检索 query 增强（补充语义关键词，提升向量召回质量）</li>
 * </ul>
 * <p>
 * 熔断降级：LLM 不可用时回退到基于正则的轻量级规则识别，保证诊断流程不中断。
 */
@Component
public class LlmIntentRecognizer implements IntentRecognizer {

    private static final Logger log = LoggerFactory.getLogger(LlmIntentRecognizer.class);

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
    private static final Pattern BRACE_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public LlmIntentRecognizer(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @CircuitBreaker(name = "llm", fallbackMethod = "fallbackRecognize")
    public DiagnosisIntent recognize(DiagnosisRequest request) {
        String userInput = composeUserInput(request);
        if (userInput.isBlank()) {
            return DiagnosisIntent.unknown("");
        }

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(userInput);

        String raw = llmClient.complete(systemPrompt, userPrompt).content();
        return parseResponse(raw, userInput);
    }

    /**
     * 熔断降级：基于正则的轻量级规则识别
     */
    @SuppressWarnings("unused")
    private DiagnosisIntent fallbackRecognize(DiagnosisRequest request, Throwable throwable) {
        log.warn("LLM intent recognition failed, falling back to rule-based recognition: {}", throwable.getMessage());
        String userInput = composeUserInput(request);
        return ruleBasedRecognize(userInput);
    }

    /**
     * 构建系统 prompt：指导 LLM 输出 JSON 格式的意图识别结果
     */
    private String buildSystemPrompt() {
        String intentList = Arrays.stream(IntentType.values())
            .map(t -> "- " + t.name() + ": " + t.getDescription())
            .collect(Collectors.joining("\n"));

        return """
            你是代码诊断系统的意图识别模块。分析用户输入的错误信息或问题描述，输出 JSON 格式的意图识别结果。

            可选意图类型：
            %s

            输出格式（严格 JSON，不要添加任何额外文本）：
            {
              "intent": "INTENT_TYPE_NAME",
              "confidence": 0.0-1.0,
              "entities": ["ClassName", "methodName", "ExceptionType", ...],
              "enhancedQuery": "用于向量检索的增强关键词，补充语义相关词"
            }

            要求：
            1. intent 必须是上述枚举值之一（大写）
            2. confidence 反映分类把握，0.0-1.0
            3. entities 提取所有关键类名、方法名、异常类型、技术栈名词
            4. enhancedQuery 用英文补充同义词和技术关键词，用于提升向量检索召回率
            """.formatted(intentList);
    }

    /**
     * 构建用户 prompt
     */
    private String buildUserPrompt(String userInput) {
        return "请分析以下错误信息并输出意图识别 JSON：\n\n" + userInput;
    }

    /**
     * 合并请求中的 query 和 errorInfo
     */
    private String composeUserInput(DiagnosisRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.query() != null && !request.query().isBlank()) {
            sb.append(request.query());
        }
        if (request.errorInfo() != null && !request.errorInfo().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(request.errorInfo());
        }
        return sb.toString().trim();
    }

    /**
     * 解析 LLM 响应为 DiagnosisIntent
     * 容错处理：LLM 可能把 JSON 包在 ```json 代码块中
     */
    private DiagnosisIntent parseResponse(String raw, String originalInput) {
        if (raw == null || raw.isBlank()) {
            log.warn("Empty LLM response for intent recognition, falling back to rule-based");
            return ruleBasedRecognize(originalInput);
        }

        String json = extractJson(raw);
        if (json == null) {
            log.warn("Failed to extract JSON from LLM response, falling back to rule-based. Raw: {}", raw);
            return ruleBasedRecognize(originalInput);
        }

        try {
            IntentResponse resp = objectMapper.readValue(json, IntentResponse.class);
            IntentType type = parseIntentType(resp.intent());
            List<String> entities = resp.entities() != null ? resp.entities() : List.of();
            String enhancedQuery = resp.enhancedQuery() != null && !resp.enhancedQuery().isBlank()
                ? resp.enhancedQuery() : originalInput;

            log.info("Intent recognized: type={}, confidence={}, entities={}", type, resp.confidence(), entities);
            return new DiagnosisIntent(type, resp.confidence(), entities, enhancedQuery);
        } catch (Exception e) {
            log.warn("Failed to parse intent JSON: {}, falling back to rule-based", e.getMessage());
            return ruleBasedRecognize(originalInput);
        }
    }

    /**
     * 从 LLM 响应中提取 JSON 字符串
     * 优先匹配 ```json 代码块，其次匹配 { ... } 大括号块
     */
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

    /**
     * 安全解析意图类型，无效值返回 UNKNOWN
     */
    private IntentType parseIntentType(String name) {
        if (name == null || name.isBlank()) {
            return IntentType.UNKNOWN;
        }
        try {
            return IntentType.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown intent type from LLM: {}", name);
            return IntentType.UNKNOWN;
        }
    }

    /**
     * 基于正则的规则识别（降级方案）
     * 关键词匹配常见异常模式，保证 LLM 不可用时仍有基本分类能力
     */
    private DiagnosisIntent ruleBasedRecognize(String input) {
        if (input == null || input.isBlank()) {
            return DiagnosisIntent.unknown("");
        }
        String lower = input.toLowerCase();

        IntentType type = IntentType.UNKNOWN;
        if (lower.contains("nullpointer") || lower.contains("null pointer") || input.contains("NullPointerException")) {
            type = IntentType.NULL_POINTER;
        } else if (lower.contains("sql") || lower.contains("jdbc") || lower.contains("database")
                || lower.contains("constraint") || lower.contains("deadlock") || lower.contains("connection refused")) {
            type = IntentType.DATABASE_ERROR;
        } else if (lower.contains("timeout") || lower.contains("socket") || lower.contains("ssl")
                || lower.contains("connection reset") || lower.contains("unknownhost")) {
            type = IntentType.NETWORK_ERROR;
        } else if (lower.contains("outofmemory") || lower.contains("oom") || lower.contains("heap")) {
            type = IntentType.MEMORY_ERROR;
        } else if (lower.contains("classnotfound") || lower.contains("noclassdeffound")
                || lower.contains("classloader")) {
            type = IntentType.CLASSLOADING;
        } else if (lower.contains("deadlock") || lower.contains("concurrent") || lower.contains("thread")
                || lower.contains("race condition")) {
            type = IntentType.CONCURRENCY;
        } else if (lower.contains("bean") || lower.contains("autowired") || lower.contains("configuration")
                || lower.contains("properties")) {
            type = IntentType.CONFIG_ERROR;
        } else if (lower.contains("illegalargument") || lower.contains("validation") || lower.contains("type mismatch")) {
            type = IntentType.API_ERROR;
        } else if (lower.contains("authentication") || lower.contains("unauthorized") || lower.contains("forbidden")) {
            type = IntentType.SECURITY;
        } else if (lower.contains("slow") || lower.contains("latency") || lower.contains("throughput")) {
            type = IntentType.PERFORMANCE;
        }

        List<String> entities = extractEntities(input);
        String enhancedQuery = buildEnhancedQuery(input, type, entities);

        log.info("Rule-based intent recognized: type={}, entities={}", type, entities);
        return new DiagnosisIntent(type, 0.6, entities, enhancedQuery);
    }

    /**
     * 从输入中提取类名/方法名/异常类型实体
     * 匹配驼峰命名的大写开头标识符和异常类名
     */
    private List<String> extractEntities(String input) {
        List<String> entities = new ArrayList<>();
        Pattern classPattern = Pattern.compile("\\b([A-Z][a-zA-Z0-9]*(?:Exception|Error)?)\\b");
        Matcher matcher = classPattern.matcher(input);
        while (matcher.find()) {
            String match = matcher.group(1);
            if (match.length() > 2 && !entities.contains(match)) {
                entities.add(match);
            }
        }
        return entities;
    }

    /**
     * 基于规则构建增强 query
     */
    private String buildEnhancedQuery(String input, IntentType type, List<String> entities) {
        StringBuilder sb = new StringBuilder(input);
        if (!entities.isEmpty()) {
            sb.append(" ").append(String.join(" ", entities));
        }
        return sb.toString();
    }

    /**
     * LLM 意图识别响应 DTO
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record IntentResponse(
        @JsonProperty("intent") String intent,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("entities") List<String> entities,
        @JsonProperty("enhancedQuery") String enhancedQuery
    ) {}
}
