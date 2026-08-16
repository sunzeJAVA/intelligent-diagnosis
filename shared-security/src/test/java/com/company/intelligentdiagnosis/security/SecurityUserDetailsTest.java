package com.company.intelligentdiagnosis.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUserDetailsTest {

    @Test
    void shouldMapAuthorities() {
        SecurityUserDetails user = new SecurityUserDetails(
            "admin", "password", List.of("ROLE_ADMIN", "ROLE_USER"), true
        );

        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getPassword()).isEqualTo("password");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.getAuthorities())
            .map(a -> a.getAuthority())
            .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void shouldSupportLockedAccount() {
        SecurityUserDetails user = new SecurityUserDetails(
            "locked", "password", List.of("ROLE_USER"), true, false, true
        );

        assertThat(user.isAccountNonLocked()).isFalse();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void shouldSupportExpiredAccount() {
        SecurityUserDetails user = new SecurityUserDetails(
            "expired", "password", List.of("ROLE_USER"), true, true, false
        );

        assertThat(user.isAccountNonExpired()).isFalse();
        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    void shouldSupportDisabledAccount() {
        SecurityUserDetails user = new SecurityUserDetails(
            "disabled", "password", List.of("ROLE_USER"), false
        );

        assertThat(user.isEnabled()).isFalse();
    }
}
