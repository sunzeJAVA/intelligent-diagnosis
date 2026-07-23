package com.company.intelligentdiagnosis.control.domain.audit;

import java.time.Instant;
import java.util.Map;

public record AuditEntry(
    String id,
    String traceId,
    String userId,
    String tenantId,
    AuditAction action,
    String resource,
    String resourceId,
    AuditResult result,
    String reason,
    Map<String, Object> context,
    String ipAddress,
    String userAgent,
    Instant timestamp,
    Instant completedAt,
    String signature
) {

    public AuditEntry withSignature(String signature) {
        return new AuditEntry(
            id, traceId, userId, tenantId, action, resource, resourceId,
            result, reason, context, ipAddress, userAgent, timestamp, completedAt, signature
        );
    }
}
