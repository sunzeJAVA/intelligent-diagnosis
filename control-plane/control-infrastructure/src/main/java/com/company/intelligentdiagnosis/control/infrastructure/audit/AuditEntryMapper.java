package com.company.intelligentdiagnosis.control.infrastructure.audit;

import com.company.intelligentdiagnosis.control.domain.audit.AuditEntry;

public final class AuditEntryMapper {

    private AuditEntryMapper() {
    }

    public static AuditEntryEntity toEntity(AuditEntry entry) {
        AuditEntryEntity entity = new AuditEntryEntity();
        entity.setId(entry.id());
        entity.setTraceId(entry.traceId());
        entity.setUserId(entry.userId());
        entity.setTenantId(entry.tenantId());
        entity.setAction(entry.action());
        entity.setResource(entry.resource());
        entity.setResourceId(entry.resourceId());
        entity.setResult(entry.result());
        entity.setReason(entry.reason());
        entity.setContext(entry.context());
        entity.setIpAddress(entry.ipAddress());
        entity.setUserAgent(entry.userAgent());
        entity.setTimestamp(entry.timestamp());
        entity.setCompletedAt(entry.completedAt());
        entity.setSignature(entry.signature());
        return entity;
    }

    public static AuditEntry toDomain(AuditEntryEntity entity) {
        return new AuditEntry(
            entity.getId(),
            entity.getTraceId(),
            entity.getUserId(),
            entity.getTenantId(),
            entity.getAction(),
            entity.getResource(),
            entity.getResourceId(),
            entity.getResult(),
            entity.getReason(),
            entity.getContext(),
            entity.getIpAddress(),
            entity.getUserAgent(),
            entity.getTimestamp(),
            entity.getCompletedAt(),
            entity.getSignature()
        );
    }
}
