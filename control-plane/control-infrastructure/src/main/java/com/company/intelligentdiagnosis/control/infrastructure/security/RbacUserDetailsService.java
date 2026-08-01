package com.company.intelligentdiagnosis.control.infrastructure.security;

import com.company.intelligentdiagnosis.control.domain.security.Role;
import com.company.intelligentdiagnosis.control.domain.security.User;
import com.company.intelligentdiagnosis.control.domain.security.UserRepository;
import com.company.intelligentdiagnosis.security.SecurityUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 RBAC 的 UserDetailsService
 */
@Component
public class RbacUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public RbacUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<String> authorities = Role.authoritiesOf(user.role()).stream().toList();
        return new SecurityUserDetails(
            user.username(),
            user.password(),
            authorities,
            user.enabled(),
            !user.isLocked(),
            true
        );
    }
}
