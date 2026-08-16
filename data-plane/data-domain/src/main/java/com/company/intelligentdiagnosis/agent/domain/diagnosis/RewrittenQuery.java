package com.company.intelligentdiagnosis.agent.domain.diagnosis;

/**
 * Query 重写结果
 * <p>
 * 一次重写可以产生两类 query：
 * <ul>
 *   <li>{@code searchQuery}：面向向量检索，强调关键词、同义词、技术栈对齐，提升召回率</li>
 *   <li>{@code llmPromptQuery}：面向 LLM 诊断 prompt，保留原始语义，更自然可读</li>
 * </ul>
 *
 * @param searchQuery    用于向量检索的 query
 * @param llmPromptQuery 用于 LLM 诊断 prompt 的 query
 */
public record RewrittenQuery(String searchQuery, String llmPromptQuery) {

    /**
     * 当不需要重写时，直接透传原始 query
     */
    public static RewrittenQuery identity(String query) {
        return new RewrittenQuery(query, query);
    }
}
