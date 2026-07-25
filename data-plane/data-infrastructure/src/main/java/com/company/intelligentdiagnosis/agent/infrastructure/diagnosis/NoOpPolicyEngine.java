package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.PolicyEngine;
import org.springframework.stereotype.Component;

/**
 * 空策略引擎实现
 * v1 版本暂不执行策略验证，预留接口供后续扩展
 */
@Component
public class NoOpPolicyEngine implements PolicyEngine {

    @Override
    public void validate(DiagnosisRequest request) {
        // v1: no policy enforcement
    }
}
