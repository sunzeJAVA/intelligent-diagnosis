package com.company.intelligentdiagnosis.agent.domain.diagnosis;

/**
 * 意图识别器接口
 * <p>
 * 对用户的诊断请求进行意图分类和实体提取，用于：
 * <ul>
 *   <li>优化向量检索的 query（提升召回质量）</li>
 *   <li>增强 LLM prompt（提供问题分类上下文）</li>
 *   <li>未来可基于意图路由到不同的检索策略</li>
 * </ul>
 */
public interface IntentRecognizer {

    /**
     * 识别诊断请求的意图
     *
     * @param request 诊断请求
     * @return 意图识别结果，识别失败时返回 {@link DiagnosisIntent#unknown(String)}
     */
    DiagnosisIntent recognize(DiagnosisRequest request);
}
