package com.company.intelligentdiagnosis.agent.domain.diagnosis;

public interface PolicyEngine {

    void validate(DiagnosisRequest request);
}
