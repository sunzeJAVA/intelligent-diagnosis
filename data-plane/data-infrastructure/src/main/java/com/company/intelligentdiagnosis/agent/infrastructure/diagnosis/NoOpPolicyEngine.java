package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.PolicyEngine;
import org.springframework.stereotype.Component;

@Component
public class NoOpPolicyEngine implements PolicyEngine {

    @Override
    public void validate(DiagnosisRequest request) {
        // v1: no policy enforcement
    }
}
