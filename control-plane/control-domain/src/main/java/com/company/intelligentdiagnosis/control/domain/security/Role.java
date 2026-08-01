package com.company.intelligentdiagnosis.control.domain.security;

import com.company.intelligentdiagnosis.security.Permission;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统角色定义
 */
public enum Role {

    ADMIN(Arrays.stream(Permission.values()).map(Permission::authority).toList()),
    OPERATOR(List.of(
        Permission.DIAGNOSIS_READ.authority(),
        Permission.DIAGNOSIS_WRITE.authority(),
        Permission.REPOSITORY_READ.authority(),
        Permission.REPOSITORY_WRITE.authority(),
        Permission.SNAPSHOT_READ.authority(),
        Permission.SNAPSHOT_ROLLBACK.authority(),
        Permission.WORKFLOW_READ.authority(),
        Permission.WORKFLOW_WRITE.authority(),
        Permission.APPROVAL_READ.authority(),
        Permission.APPROVAL_WRITE.authority(),
        Permission.AUDIT_READ.authority()
    )),
    VIEWER(List.of(
        Permission.DIAGNOSIS_READ.authority(),
        Permission.REPOSITORY_READ.authority(),
        Permission.SNAPSHOT_READ.authority(),
        Permission.WORKFLOW_READ.authority(),
        Permission.APPROVAL_READ.authority()
    ));

    private final Set<String> authorities;

    Role(List<String> authorities) {
        this.authorities = Collections.unmodifiableSet(Set.copyOf(authorities));
    }

    public Set<String> authorities() {
        return authorities;
    }

    public static Set<String> authoritiesOf(String roleName) {
        return Arrays.stream(values())
            .filter(r -> r.name().equalsIgnoreCase(roleName))
            .findFirst()
            .map(Role::authorities)
            .orElse(Collections.emptySet());
    }
}
