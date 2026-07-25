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
}
