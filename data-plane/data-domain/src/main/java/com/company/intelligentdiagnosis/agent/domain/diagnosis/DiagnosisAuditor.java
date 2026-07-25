package com.company.intelligentdiagnosis.agent.domain.diagnosis;

/**
 * 诊断审计接口
 * 记录诊断请求和响应的审计日志
 */
public interface DiagnosisAuditor {

    /**
     * 记录审计日志
     *
     * @param request        诊断请求
     * @param response       诊断响应
     * @param durationMillis 诊断耗时（毫秒）
     */
    void record(DiagnosisRequest request, DiagnosisResponse response, long durationMillis);
}
