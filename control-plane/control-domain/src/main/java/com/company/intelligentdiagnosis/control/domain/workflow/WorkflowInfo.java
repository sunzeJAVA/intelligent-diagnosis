package com.company.intelligentdiagnosis.control.domain.workflow;

import java.time.Instant;

public record WorkflowInfo(
    String workflowId,
    String workflowType,
    String status,
    String currentStep,
    Instant startedAt,
    Instant completedAt,
    String repositoryId,
    String repositoryName,
    String commitHash
) {
}
