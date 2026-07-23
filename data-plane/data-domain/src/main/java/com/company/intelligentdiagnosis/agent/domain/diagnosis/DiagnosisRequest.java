package com.company.intelligentdiagnosis.agent.domain.diagnosis;

public record DiagnosisRequest(
    String query,
    String errorInfo,
    String service,
    String userId,
    String tenantId
) {
}
