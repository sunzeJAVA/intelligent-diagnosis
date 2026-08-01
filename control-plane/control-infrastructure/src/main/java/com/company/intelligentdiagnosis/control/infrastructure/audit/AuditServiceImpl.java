package com.company.intelligentdiagnosis.control.infrastructure.audit;

import com.company.intelligentdiagnosis.control.domain.audit.AuditAction;
import com.company.intelligentdiagnosis.control.domain.audit.AuditEntry;
import com.company.intelligentdiagnosis.control.domain.audit.AuditResult;
import com.company.intelligentdiagnosis.control.domain.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditEntryRepository auditEntryRepository;

    public AuditServiceImpl(AuditEntryRepository auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    @Override
    @Transactional
    public String startAudit(String action, String resource, String resourceId, Map<String, Object> context) {
        String auditId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        String signature = signEntry(auditId, action, timestamp, resource);

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
            timestamp,
            null,
            signature
        );

        auditEntryRepository.save(AuditEntryMapper.toEntity(entry));
        log.debug("Started audit: {}", auditId);

        return auditId;
    }

    @Override
    @Transactional
    public void completeAudit(String auditId, AuditResult result, String reason) {
        completeAudit(auditId, result, Map.of("reason", reason));
    }

    @Override
    @Transactional
    public void completeAudit(String auditId, AuditResult result, Map<String, Object> details) {
        Optional<AuditEntryEntity> optional = auditEntryRepository.findById(auditId);
        if (optional.isEmpty()) {
            log.warn("Audit entry not found for completion: {}", auditId);
            return;
        }

        AuditEntryEntity entity = optional.get();
        entity.setResult(result);
        if (details != null && details.get("reason") != null) {
            entity.setReason(details.get("reason").toString());
        }
        entity.setContext(details);
        entity.setCompletedAt(Instant.now());
        entity.setSignature(signEntry(
            entity.getId(),
            entity.getAction().name(),
            entity.getTimestamp(),
            entity.getResource()
        ));

        auditEntryRepository.save(entity);
        log.debug("Completed audit: {} with result: {}", auditId, result);
    }

    @Override
    @Transactional
    public void failAudit(String auditId, String reason) {
        completeAudit(auditId, AuditResult.FAILURE, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntry> getAuditsByResource(String resource) {
        return auditEntryRepository.findByResourceOrderByTimestampDesc(resource).stream()
            .map(AuditEntryMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntry> getAuditsByUser(String userId) {
        return auditEntryRepository.findByUserIdOrderByTimestampDesc(userId).stream()
            .map(AuditEntryMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyIntegrity(String auditId) {
        Optional<AuditEntryEntity> optional = auditEntryRepository.findById(auditId);
        if (optional.isEmpty()) {
            return false;
        }

        AuditEntryEntity entity = optional.get();
        String expected = signEntry(
            entity.getId(),
            entity.getAction().name(),
            entity.getTimestamp(),
            entity.getResource()
        );
        return expected.equals(entity.getSignature());
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
