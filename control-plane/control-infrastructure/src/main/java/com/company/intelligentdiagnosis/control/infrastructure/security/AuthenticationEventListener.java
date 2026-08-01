package com.company.intelligentdiagnosis.control.infrastructure.security;

import com.company.intelligentdiagnosis.control.domain.audit.AuditAction;
import com.company.intelligentdiagnosis.control.domain.audit.AuditResult;
import com.company.intelligentdiagnosis.control.domain.audit.AuditService;
import com.company.intelligentdiagnosis.control.domain.security.User;
import com.company.intelligentdiagnosis.control.domain.security.UserRepository;
import com.company.intelligentdiagnosis.security.SecurityProperties;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

/**
 * 认证事件监听器
 * 记录登录审计、处理账户锁定策略
 */
@Component
public class AuthenticationEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventListener.class);

    private final AuditService auditService;
    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter accountLockedCounter;

    public AuthenticationEventListener(AuditService auditService,
                                       UserRepository userRepository,
                                       SecurityProperties securityProperties,
                                       Counter securityLoginSuccessCounter,
                                       Counter securityLoginFailureCounter,
                                       Counter securityAccountLockedCounter) {
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.securityProperties = securityProperties;
        this.loginSuccessCounter = securityLoginSuccessCounter;
        this.loginFailureCounter = securityLoginFailureCounter;
        this.accountLockedCounter = securityAccountLockedCounter;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();
        String ipAddress = extractIpAddress(authentication);
        String userAgent = extractUserAgent(authentication);

        auditService.startAudit(
            AuditAction.LOGIN_SUCCESS.name(),
            "auth",
            username,
            Map.of(
                "ipAddress", ipAddress != null ? ipAddress : "unknown",
                "userAgent", userAgent != null ? userAgent : "unknown"
            )
        );
        loginSuccessCounter.increment();

        resetLockout(username);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication != null ? authentication.getName() : "unknown";
        String ipAddress = extractIpAddress(authentication);
        String userAgent = extractUserAgent(authentication);
        String reason = event.getException() != null ? event.getException().getMessage() : "Authentication failed";

        String auditId = auditService.startAudit(
            AuditAction.LOGIN_FAILURE.name(),
            "auth",
            username,
            Map.of(
                "ipAddress", ipAddress != null ? ipAddress : "unknown",
                "userAgent", userAgent != null ? userAgent : "unknown",
                "reason", reason
            )
        );
        auditService.completeAudit(auditId, AuditResult.FAILURE, reason);
        loginFailureCounter.increment();

        if (event instanceof AuthenticationFailureLockedEvent) {
            auditService.startAudit(
                AuditAction.ACCOUNT_LOCKED.name(),
                "auth",
                username,
                Map.of("reason", "Account is locked", "ipAddress", ipAddress != null ? ipAddress : "unknown")
            );
            accountLockedCounter.increment();
            return;
        }

        if (event instanceof AuthenticationFailureBadCredentialsEvent) {
            handleBadCredentials(username);
        }
    }

    private void handleBadCredentials(String username) {
        if (username == null || username.isBlank() || "unknown".equals(username)) {
            return;
        }

        Optional<User> optional = userRepository.findByUsername(username);
        if (optional.isEmpty()) {
            return;
        }

        User user = optional.get();
        int attempts = user.failedLoginAttempts() + 1;
        int maxAttempts = securityProperties.getLockout().getMaxAttempts();
        Instant lockedUntil = null;

        if (attempts >= maxAttempts) {
            int durationMinutes = securityProperties.getLockout().getDurationMinutes();
            lockedUntil = Instant.now().plus(durationMinutes, ChronoUnit.MINUTES);
            attempts = maxAttempts;
            log.warn("Account locked for user {} until {}", username, lockedUntil);

            auditService.startAudit(
                AuditAction.ACCOUNT_LOCKED.name(),
                "auth",
                username,
                Map.of(
                    "lockedUntil", lockedUntil.toString(),
                    "failedAttempts", attempts,
                    "durationMinutes", durationMinutes
                )
            );
            accountLockedCounter.increment();
        }

        User updated = user.withFailedLogin(attempts, Instant.now(), lockedUntil);
        userRepository.updateLockout(updated);
    }

    private void resetLockout(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.failedLoginAttempts() > 0 || user.lockedUntil() != null) {
                userRepository.updateLockout(user.withLockoutCleared());
            }
        });
    }

    private String extractIpAddress(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object details = authentication.getDetails();
        if (details instanceof WebAuthenticationDetails webDetails) {
            return webDetails.getRemoteAddress();
        }
        return null;
    }

    private String extractUserAgent(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object details = authentication.getDetails();
        if (details instanceof WebAuthenticationDetails) {
            // WebAuthenticationDetails 不携带 User-Agent，仅记录 IP
            return null;
        }
        return null;
    }
}
