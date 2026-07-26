package com.company.intelligentdiagnosis.control.infrastructure.audit;

import com.company.intelligentdiagnosis.control.domain.audit.AuditAction;
import com.company.intelligentdiagnosis.control.domain.audit.AuditEntry;
import com.company.intelligentdiagnosis.control.domain.audit.AuditResult;
import com.company.intelligentdiagnosis.control.domain.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final Map<String, AuditEntry> auditStore = new ConcurrentHashMap<>();

    @Override
    public String startAudit(String action, String resource, String resourceId, Map<String, Object> context) {
        String auditId = UUID.randomUUID().toString();
        String signature = signEntry(auditId, action, Instant.now(), resource);
        
        AuditEntry entry = new AuditEntry(
            auditId,
            null,
            "admin",
            "default",
            AuditAction.valueOf(action),
            resource,
            resourceId,
            null,
            null,
            context,
            null,
            null,
            Instant.now(),
            null,
            signature
        );
        
        auditStore.put(auditId, entry);
        log.debug("Started audit: {}", auditId);

        return auditId;
    }

    @Override
    public void completeAudit(String auditId, AuditResult result, String reason) {
        completeAudit(auditId, result, Map.of("reason", reason));
    }

    @Override
    public void completeAudit(String auditId, AuditResult result, Map<String, Object> details) {
        auditStore.computeIfPresent(auditId, (id, entry) -> {
            String signature = signEntry(id, entry.action().name(), entry.timestamp(), entry.resource());
            return new AuditEntry(
                id,
                entry.traceId(),
                entry.userId(),
                entry.tenantId(),
                entry.action(),
                entry.resource(),
                entry.resourceId(),
                result,
                details != null ? details.get("reason") != null ? details.get("reason").toString() : null : null,
                details,
                entry.ipAddress(),
                entry.userAgent(),
                entry.timestamp(),
                Instant.now(),
                signature
            );
        });
        log.debug("Completed audit: {} with result: {}", auditId, result);
    }

    @Override
    public void failAudit(String auditId, String reason) {
        completeAudit(auditId, AuditResult.FAILURE, reason);
    }

    @Override
    public List<AuditEntry> getAuditsByResource(String resource) {
        return auditStore.values().stream()
            .filter(entry -> resource.equals(entry.resource()))
            .collect(Collectors.toList());
    }

    @Override
    public List<AuditEntry> getAuditsByUser(String userId) {
        return auditStore.values().stream()
            .filter(entry -> userId.equals(entry.userId()))
            .collect(Collectors.toList());
    }

    @Override
    public boolean verifyIntegrity(String auditId) {
        return auditStore.containsKey(auditId);
    }

    private String signEntry(String id, String action, Instant timestamp, String resource) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String signable = id + action + timestamp + resource;
            byte[] hash = digest.digest(signable.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to sign audit entry", e);
            return null;
        }
    }
}
