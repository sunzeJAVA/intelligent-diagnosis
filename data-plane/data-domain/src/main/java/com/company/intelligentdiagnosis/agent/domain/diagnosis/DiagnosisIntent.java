package com.company.intelligentdiagnosis.agent.domain.diagnosis;

import java.util.List;

/**
 * 诊断意图识别结果
 * <p>
 * 由 {@link IntentRecognizer} 对用户输入进行分析后产生，包含：
 * <ul>
 *   <li>意图分类（问题类型）</li>
 *   <li>置信度（0.0-1.0）</li>
 *   <li>提取的关键实体（类名、方法名、异常类型等）</li>
 *   <li>增强后的检索关键词（用于优化向量检索）</li>
 * </ul>
 *
 * @param type           意图类型
 * @param confidence     置信度，0.0-1.0
 * @param entities       提取的关键实体列表（类名、方法名、异常类等）
 * @param enhancedQuery  增强后的检索关键词，用于 CodeRetriever 提升召回质量
 */
public record DiagnosisIntent(
    IntentType type,
    double confidence,
    List<String> entities,
    String enhancedQuery
) {

    /**
     * 创建一个未知意图的默认结果
     *
     * @param originalQuery 用户原始查询文本
     * @return 未知意图
     */
    public static DiagnosisIntent unknown(String originalQuery) {
        return new DiagnosisIntent(IntentType.UNKNOWN, 0.0, List.of(), originalQuery);
    }
}
