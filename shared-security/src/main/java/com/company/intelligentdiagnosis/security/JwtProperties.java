package com.company.intelligentdiagnosis.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性
 */
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * HS256 密钥，必须通过环境变量或配置文件注入，长度不少于 32 字节（256 位）
     */
    private String secret;

    /**
     * Token 有效期（毫秒），默认 24 小时
     */
    private long expirationMs = 24 * 60 * 60 * 1000L;

    /**
     * 校验密钥是否已配置且长度满足要求
     */
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT secret is required. Set security.jwt.secret or JWT_SECRET environment variable."
            );
        }
        if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long.");
        }
    }

    /**
     * Token 签发者
     */
    private String issuer = "intelligent-diagnosis";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
