package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.IntentType;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.QueryRewriter;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.RewrittenQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于规则的 Query 重写器
 * <p>
 * 不依赖 LLM，通过本地映射和正则做轻量级 query 增强：
 * <ul>
 *   <li>技术栈缩写扩展（NPE → NullPointerException）</li>
 *   <li>中文技术术语同义词转换（空指针 → NullPointerException）</li>
 *   <li>意图类型关键词补充</li>
 *   <li>类名/异常名实体保留</li>
 * </ul>
 * 适合作为 LLM 重写失败时的降级方案，也适合对延迟敏感的场景。
 */
@Component
public class RuleBasedQueryRewriter implements QueryRewriter {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedQueryRewriter.class);

    private static final Pattern CLASS_PATTERN = Pattern.compile("\\b([A-Z][a-zA-Z0-9]*(?:Exception|Error)?)\\b");

    private static final Map<String, String> ABBREVIATION_MAP = new LinkedHashMap<>();

    static {
        ABBREVIATION_MAP.put("npe", "NullPointerException");
        ABBREVIATION_MAP.put("oom", "OutOfMemoryError");
        ABBREVIATION_MAP.put("sofe", "StackOverflowError");
        ABBREVIATION_MAP.put("jdbc", "JDBC");
        ABBREVIATION_MAP.put("sql", "SQL");
        ABBREVIATION_MAP.put("ssl", "SSL TLS");
        ABBREVIATION_MAP.put("jvm", "JVM");
        ABBREVIATION_MAP.put("gc", "GC garbage collection");
        ABBREVIATION_MAP.put("feign", "FeignClient");
        ABBREVIATION_MAP.put("rest", "REST");
        ABBREVIATION_MAP.put("api", "API");
        ABBREVIATION_MAP.put("bean", "Spring Bean");
    }

    /**
     * 中文技术术语 → 英文技术术语同义词映射
     * 键为中文关键词，值为对应的英文技术术语/同义词
     */
    private static final Map<String, String> CHINESE_SYNONYM_MAP = new LinkedHashMap<>();

    static {
        CHINESE_SYNONYM_MAP.put("空指针", "NullPointerException null object reference");
        CHINESE_SYNONYM_MAP.put("内存溢出", "OutOfMemoryError OOM heap memory");
        CHINESE_SYNONYM_MAP.put("栈溢出", "StackOverflowError");
        CHINESE_SYNONYM_MAP.put("类找不到", "ClassNotFoundException");
        CHINESE_SYNONYM_MAP.put("类定义找不到", "NoClassDefFoundError");
        CHINESE_SYNONYM_MAP.put("数据库", "SQL database JDBC");
        CHINESE_SYNONYM_MAP.put("连接超时", "connection timeout");
        CHINESE_SYNONYM_MAP.put("网络超时", "network timeout socket");
        CHINESE_SYNONYM_MAP.put("死锁", "deadlock");
        CHINESE_SYNONYM_MAP.put("并发", "concurrent concurrency");
        CHINESE_SYNONYM_MAP.put("线程安全", "thread safety race condition");
        CHINESE_SYNONYM_MAP.put("配置错误", "configuration error property binding");
        CHINESE_SYNONYM_MAP.put("bean", "Spring Bean");
        CHINESE_SYNONYM_MAP.put("注入失败", "autowired dependency injection failed");
        CHINESE_SYNONYM_MAP.put("参数错误", "IllegalArgumentException parameter validation");
        CHINESE_SYNONYM_MAP.put("认证失败", "authentication failed");
        CHINESE_SYNONYM_MAP.put("授权失败", "authorization forbidden");
        CHINESE_SYNONYM_MAP.put("性能问题", "performance slow latency throughput");
        CHINESE_SYNONYM_MAP.put("响应慢", "slow response latency");
        CHINESE_SYNONYM_MAP.put("吞吐量", "throughput");
    }

    private static final Map<IntentType, String> INTENT_KEYWORDS = new LinkedHashMap<>();

    static {
        INTENT_KEYWORDS.put(IntentType.NULL_POINTER, "null object reference NullPointerException");
        INTENT_KEYWORDS.put(IntentType.DATABASE_ERROR, "SQL database connection transaction deadlock constraint");
        INTENT_KEYWORDS.put(IntentType.NETWORK_ERROR, "network timeout socket connection refused SSL");
        INTENT_KEYWORDS.put(IntentType.CONCURRENCY, "concurrent deadlock thread race condition lock");
        INTENT_KEYWORDS.put(IntentType.MEMORY_ERROR, "OutOfMemoryError heap memory leak GC");
        INTENT_KEYWORDS.put(IntentType.CONFIG_ERROR, "configuration property bean autowired binding");
        INTENT_KEYWORDS.put(IntentType.CLASSLOADING, "ClassNotFoundException NoClassDefFoundError classloader dependency");
        INTENT_KEYWORDS.put(IntentType.API_ERROR, "IllegalArgumentException validation parameter type mismatch");
        INTENT_KEYWORDS.put(IntentType.PERFORMANCE, "performance slow latency throughput bottleneck");
        INTENT_KEYWORDS.put(IntentType.SECURITY, "authentication authorization forbidden security");
    }

    @Override
    public RewrittenQuery rewrite(String originalQuery, DiagnosisIntent intent) {
        if (originalQuery == null || originalQuery.isBlank()) {
            return RewrittenQuery.identity("");
        }

        String normalized = originalQuery.trim();
        String lower = normalized.toLowerCase();

        List<String> expansions = new ArrayList<>();

        // 1. 缩写扩展
        for (Map.Entry<String, String> entry : ABBREVIATION_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                expansions.add(entry.getValue());
            }
        }

        // 2. 中文技术术语同义词转换
        for (Map.Entry<String, String> entry : CHINESE_SYNONYM_MAP.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                expansions.add(entry.getValue());
            }
        }

        // 3. 根据意图类型补充关键词
        String intentKeywords = INTENT_KEYWORDS.get(intent.type());
        if (intentKeywords != null) {
            expansions.add(intentKeywords);
        }

        // 3. 提取并保留实体
        List<String> entities = extractEntities(normalized);
        if (!entities.isEmpty()) {
            expansions.add(String.join(" ", entities));
        }

        String searchQuery = buildSearchQuery(normalized, expansions);
        String llmPromptQuery = buildLlmPromptQuery(normalized, intent, entities);

        log.debug("Rule-based query rewrite: original='{}', searchQuery='{}'", normalized, searchQuery);
        return new RewrittenQuery(searchQuery, llmPromptQuery);
    }

    private List<String> extractEntities(String input) {
        List<String> entities = new ArrayList<>();
        Matcher matcher = CLASS_PATTERN.matcher(input);
        while (matcher.find()) {
            String match = matcher.group(1);
            if (match.length() > 2 && !entities.contains(match)) {
                entities.add(match);
            }
        }
        return entities;
    }

    private String buildSearchQuery(String original, List<String> expansions) {
        StringBuilder sb = new StringBuilder(original);
        for (String expansion : expansions) {
            sb.append(" ").append(expansion);
        }
        return sb.toString();
    }

    private String buildLlmPromptQuery(String original, DiagnosisIntent intent, List<String> entities) {
        StringBuilder sb = new StringBuilder(original);
        if (!entities.isEmpty()) {
            sb.append(" | Key entities: ").append(String.join(", ", entities));
        }
        sb.append(" | Category: ").append(intent.type().getDisplayName());
        return sb.toString();
    }
}
