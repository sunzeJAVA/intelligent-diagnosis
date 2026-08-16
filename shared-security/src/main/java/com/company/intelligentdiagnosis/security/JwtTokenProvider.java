package com.company.intelligentdiagnosis.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * JWT 令牌生成与解析
 */
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String AUTHORITIES_KEY = "authorities";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 为用户生成 JWT
     */
    public String generateToken(UserDetails userDetails) {
        List<String> authorities = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getExpirationMs());

        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuer(properties.getIssuer())
            .issuedAt(now)
            .expiration(expiry)
            .claim(AUTHORITIES_KEY, authorities)
            .signWith(key)
            .compact();
    }

    /**
     * 从 Token 中解析用户名
     */
    public Optional<String> extractUsername(String token) {
        return parseClaims(token).map(Claims::getSubject);
    }

    /**
     * 从 Token 中解析权限列表
     */
    public List<GrantedAuthority> extractAuthorities(String token) {
        return parseClaims(token)
            .map(claims -> {
                @SuppressWarnings("unchecked")
                List<String> authorities = claims.get(AUTHORITIES_KEY, List.class);
                if (authorities == null) {
                    return List.<GrantedAuthority>of();
                }
                return authorities.stream()
                    .map(SimpleGrantedAuthority::new)
                    .map(a -> (GrantedAuthority) a)
                    .toList();
            })
            .orElse(List.of());
    }

    /**
     * 校验 Token 是否有效
     */
    public boolean validateToken(String token) {
        boolean valid = parseClaims(token).isPresent();
        if (!valid) {
            log.warn("Invalid JWT token");
        }
        return valid;
    }

    private Optional<Claims> parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
