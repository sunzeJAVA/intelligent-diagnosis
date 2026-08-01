package com.company.intelligentdiagnosis.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 用于 Spring Security 的用户详情实现
 * 支持账户锁定与过期状态
 */
public class SecurityUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final List<SimpleGrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean accountNonExpired;

    public SecurityUserDetails(String username,
                               String password,
                               Collection<String> authorities,
                               boolean enabled) {
        this(username, password, authorities, enabled, true, true);
    }

    public SecurityUserDetails(String username,
                               String password,
                               Collection<String> authorities,
                               boolean enabled,
                               boolean accountNonLocked,
                               boolean accountNonExpired) {
        this.username = username;
        this.password = password;
        this.authorities = authorities.stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.accountNonExpired = accountNonExpired;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
