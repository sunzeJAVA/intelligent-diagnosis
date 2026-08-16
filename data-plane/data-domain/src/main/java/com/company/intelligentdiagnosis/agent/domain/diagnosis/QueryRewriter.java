package com.company.intelligentdiagnosis.agent.domain.diagnosis;

/**
 * Query 重写器接口
 * <p>
 * 基于原始查询和意图识别结果，生成更适合向量检索和 LLM 分析的查询文本。
 * 职责与 {@link IntentRecognizer} 分离：意图识别聚焦分类和实体提取，
 * query 重写专注把用户输入转化为高召回、语义完整的检索词。
 */
public interface QueryRewriter {

    /**
     * 重写查询
     *
     * @param originalQuery 用户原始 query
     * @param intent        意图识别结果
     * @return 重写后的查询，包含用于检索的 searchQuery 和用于 prompt 的 llmPromptQuery
     */
    RewrittenQuery rewrite(String originalQuery, DiagnosisIntent intent);
}
