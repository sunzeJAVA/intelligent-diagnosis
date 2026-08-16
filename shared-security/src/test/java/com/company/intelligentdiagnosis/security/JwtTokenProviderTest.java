package com.company.intelligentdiagnosis.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "this-is-a-strong-jwt-secret-key-32bytes";

    private final JwtProperties properties = createProperties();
    private final JwtTokenProvider provider = new JwtTokenProvider(properties);

    private JwtProperties createProperties() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpirationMs(60_000L);
        props.setIssuer("test-issuer");
        return props;
    }

    @Test
    void shouldGenerateAndValidateToken() {
        SecurityUserDetails user = new SecurityUserDetails(
            "admin", "password", List.of("ROLE_ADMIN", "ROLE_USER"), true
        );

        String token = provider.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.extractUsername(token)).hasValue("admin");
        assertThat(provider.extractAuthorities(token))
            .map(a -> a.getAuthority())
            .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThat(provider.validateToken("not.a.token")).isFalse();
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        SecurityUserDetails user = new SecurityUserDetails("admin", "password", List.of("ROLE_USER"), true);
        String token = provider.generateToken(user);

        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("different-secret-key-of-32-bytes-long!!");
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProps);

        assertThat(otherProvider.validateToken(token)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret(SECRET);
        shortProps.setExpirationMs(1L);
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortProps);

        SecurityUserDetails user = new SecurityUserDetails("admin", "password", List.of("ROLE_USER"), true);
        String token = shortProvider.generateToken(user);

        Thread.sleep(10);

        assertThat(shortProvider.validateToken(token)).isFalse();
    }

    @Test
    void shouldReturnEmptyAuthoritiesForInvalidToken() {
        assertThat(provider.extractAuthorities("invalid-token")).isEmpty();
    }

    @Test
    void shouldReturnEmptyUsernameForInvalidToken() {
        assertThat(provider.extractUsername("invalid-token")).isEmpty();
    }
}
