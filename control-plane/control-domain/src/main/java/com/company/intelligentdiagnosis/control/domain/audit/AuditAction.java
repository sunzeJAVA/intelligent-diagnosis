package com.company.intelligentdiagnosis.control.domain.audit;

public enum AuditAction {
    CODE_PARSE,
    INDEX_CREATE,
    INDEX_UPDATE,
    INDEX_DELETE,
    INDEX_ROLLBACK,
    DIAGNOSIS_REQUEST,
    DIAGNOSIS_APPROVE,
    DIAGNOSIS_EXPORT,
    CONFIG_READ,
    CONFIG_WRITE,
    POLICY_CHANGE
}
