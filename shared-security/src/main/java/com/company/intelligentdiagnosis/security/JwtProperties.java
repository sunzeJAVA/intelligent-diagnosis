package com.company.intelligentdiagnosis.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性
 */
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * HS256 密钥，建议通过环境变量注入，长度不少于 256 位
     */
    private String secret = System.getenv("JWT_SECRET") != null ? System.getenv("JWT_SECRET") : "change-me-in-production-32chars-long!";

    /**
     * Token 有效期（毫秒），默认 24 小时
     */
    private long expirationMs = 24 * 60 * 60 * 1000L;

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
