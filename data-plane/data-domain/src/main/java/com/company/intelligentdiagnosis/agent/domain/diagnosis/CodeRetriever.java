package com.company.intelligentdiagnosis.agent.domain.diagnosis;

import java.util.List;

/**
 * 代码检索接口
 * 根据诊断请求检索相关的代码片段
 */
public interface CodeRetriever {

    /**
     * 检索相关代码片段
     *
     * @param request 诊断请求
     * @return 相关代码片段列表，无结果时返回空列表
     */
    List<CodeSnippet> retrieve(DiagnosisRequest request);

    /**
     * 根据诊断请求和意图识别结果检索相关代码片段
     * <p>
     * 支持多路召回的实现可以利用意图中的关键实体（类名、方法名等）做更精确的检索。
     * 默认实现委托给单参数方法，保持向后兼容。
     *
     * @param request 诊断请求
     * @param intent  意图识别结果
     * @return 相关代码片段列表，无结果时返回空列表
     */
    default List<CodeSnippet> retrieve(DiagnosisRequest request, DiagnosisIntent intent) {
        return retrieve(request);
    }
}
