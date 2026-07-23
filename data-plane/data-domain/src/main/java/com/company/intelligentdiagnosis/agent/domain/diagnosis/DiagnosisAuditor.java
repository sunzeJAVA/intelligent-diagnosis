package com.company.intelligentdiagnosis.agent.domain.diagnosis;

public interface DiagnosisAuditor {

    void record(DiagnosisRequest request, DiagnosisResponse response, long durationMillis);
}
