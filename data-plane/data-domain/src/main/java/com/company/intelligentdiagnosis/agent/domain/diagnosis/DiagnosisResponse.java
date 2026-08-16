package com.company.intelligentdiagnosis.agent.domain.diagnosis;

import java.util.List;

/**
 * 诊断响应
 * 包含诊断结果的所有输出信息
 *
 * @param summary     诊断摘要
 * @param rootCause   根因分析
 * @param suggestions 修复建议列表
 * @param relatedCode 相关代码片段列表
 * @param intent      意图识别结果（问题分类、置信度、关键实体），可为 null
 */
public record DiagnosisResponse(
    String summary,
    String rootCause,
    List<String> suggestions,
    List<CodeSnippet> relatedCode,
    DiagnosisIntent intent,
    boolean degraded
) {

    /**
     * 向后兼容的工厂方法：不携带意图识别结果，非降级
     */
    public static DiagnosisResponse of(String summary,
                                       String rootCause,
                                       List<String> suggestions,
                                       List<CodeSnippet> relatedCode) {
        return new DiagnosisResponse(summary, rootCause, suggestions, relatedCode, null, false);
    }

    /**
     * 向后兼容的工厂方法：携带意图识别结果，非降级
     */
    public static DiagnosisResponse of(String summary,
                                       String rootCause,
                                       List<String> suggestions,
                                       List<CodeSnippet> relatedCode,
                                       DiagnosisIntent intent) {
        return new DiagnosisResponse(summary, rootCause, suggestions, relatedCode, intent, false);
    }
}
