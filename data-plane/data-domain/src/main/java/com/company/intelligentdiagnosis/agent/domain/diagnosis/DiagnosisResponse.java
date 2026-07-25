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
 */
public record DiagnosisResponse(
    String summary,
    String rootCause,
    List<String> suggestions,
    List<CodeSnippet> relatedCode
) {
}
