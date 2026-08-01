package com.company.intelligentdiagnosis.security;

/**
 * 系统权限常量
 * 用于 @PreAuthorize 与方法级访问控制
 */
public enum Permission {

    DIAGNOSIS_READ("diagnosis:read"),
    DIAGNOSIS_WRITE("diagnosis:write"),

    REPOSITORY_READ("repository:read"),
    REPOSITORY_WRITE("repository:write"),

    SNAPSHOT_READ("snapshot:read"),
    SNAPSHOT_ROLLBACK("snapshot:rollback"),

    WORKFLOW_READ("workflow:read"),
    WORKFLOW_WRITE("workflow:write"),

    APPROVAL_READ("approval:read"),
    APPROVAL_WRITE("approval:write"),

    AUDIT_READ("audit:read"),

    ADMIN_READ("admin:read"),
    ADMIN_WRITE("admin:write");

    private final String authority;

    Permission(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
