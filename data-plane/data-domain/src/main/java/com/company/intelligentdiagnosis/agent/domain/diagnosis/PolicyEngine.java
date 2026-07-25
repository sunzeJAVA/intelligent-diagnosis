package com.company.intelligentdiagnosis.agent.domain.diagnosis;

/**
 * 策略引擎接口
 * 用于验证诊断请求是否符合安全策略和业务规则
 */
public interface PolicyEngine {

    /**
     * 验证诊断请求
     *
     * @param request 诊断请求
     * @throws RuntimeException 当请求不符合策略时抛出异常
     */
    void validate(DiagnosisRequest request);
}
