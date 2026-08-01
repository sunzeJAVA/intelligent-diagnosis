package com.company.intelligentdiagnosis.control.domain.security;

import java.time.Instant;

/**
 * 用户领域对象
 */
public record User(
    String username,
    String password,
    String role,
    boolean enabled,
    int failedLoginAttempts,
    Instant lastFailedLoginAt,
    Instant lockedUntil,
    Instant updatedAt
) {

    public User(String username, String password, String role, boolean enabled) {
        this(username, password, role, enabled, 0, null, null, Instant.now());
    }

    public User withFailedLogin(int failedLoginAttempts, Instant lastFailedLoginAt, Instant lockedUntil) {
        return new User(
            username, password, role, enabled,
            failedLoginAttempts, lastFailedLoginAt, lockedUntil, Instant.now()
        );
    }

    public User withLockoutCleared() {
        return new User(
            username, password, role, enabled,
            0, null, null, Instant.now()
        );
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }
}
