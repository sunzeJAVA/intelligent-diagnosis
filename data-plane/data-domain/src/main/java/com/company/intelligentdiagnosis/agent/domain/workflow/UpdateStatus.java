package com.company.intelligentdiagnosis.agent.domain.workflow;

public enum UpdateStatus {
    PENDING,
    RUNNING,
    PAUSED,
    AWAITING_APPROVAL,
    APPROVED,
    REJECTED,
    COMPLETED,
    FAILED,
    ROLLED_BACK
}
