package com.company.intelligentdiagnosis.control.domain.security;

/**
 * 用户领域对象
 */
public record User(
    String username,
    String password,
    String role,
    boolean enabled
) {
}
