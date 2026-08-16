package com.company.intelligentdiagnosis.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void shouldAcceptSecretOfAtLeast32Bytes() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("this-secret-is-exactly-32-bytes-long");

        properties.validate();

        assertThat(properties.getSecret()).isEqualTo("this-secret-is-exactly-32-bytes-long");
    }

    @Test
    void shouldRejectBlankSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("  ");

        assertThatThrownBy(properties::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT secret is required");
    }

    @Test
    void shouldRejectNullSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(null);

        assertThatThrownBy(properties::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT secret is required");
    }

    @Test
    void shouldRejectSecretShorterThan32Bytes() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("short-secret");

        assertThatThrownBy(properties::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void shouldHaveDefaultExpirationOfOneDay() {
        JwtProperties properties = new JwtProperties();

        assertThat(properties.getExpirationMs()).isEqualTo(24 * 60 * 60 * 1000L);
    }

    @Test
    void shouldHaveDefaultIssuer() {
        JwtProperties properties = new JwtProperties();

        assertThat(properties.getIssuer()).isEqualTo("intelligent-diagnosis");
    }
}
