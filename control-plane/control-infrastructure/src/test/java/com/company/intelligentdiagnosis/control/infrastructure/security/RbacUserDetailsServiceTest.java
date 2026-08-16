package com.company.intelligentdiagnosis.control.infrastructure.security;

import com.company.intelligentdiagnosis.control.domain.security.User;
import com.company.intelligentdiagnosis.control.domain.security.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void shouldLoadAdminUserWithAllPermissions() {
        RbacUserDetailsService service = new RbacUserDetailsService(userRepository);
        User admin = new User("admin", "encoded-password", "ADMIN", true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.getAuthorities())
            .map(a -> a.getAuthority())
            .contains("admin:read", "admin:write");
    }

    @Test
    void shouldLoadViewerUserWithReadOnlyPermissions() {
        RbacUserDetailsService service = new RbacUserDetailsService(userRepository);
        User viewer = new User("viewer", "encoded-password", "VIEWER", true);
        when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(viewer));

        UserDetails details = service.loadUserByUsername("viewer");

        assertThat(details.getAuthorities())
            .map(a -> a.getAuthority())
            .contains(
                "diagnosis:read",
                "repository:read",
                "workflow:read"
            );
    }

    @Test
    void shouldMapLockedUserToAccountNonLockedFalse() {
        RbacUserDetailsService service = new RbacUserDetailsService(userRepository);
        User locked = new User("locked", "encoded-password", "OPERATOR", true)
            .withFailedLogin(5, null, java.time.Instant.now().plusSeconds(3600));
        when(userRepository.findByUsername("locked")).thenReturn(Optional.of(locked));

        UserDetails details = service.loadUserByUsername("locked");

        assertThat(details.isAccountNonLocked()).isFalse();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        RbacUserDetailsService service = new RbacUserDetailsService(userRepository);
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found");
    }
}
