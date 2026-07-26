package com.company.intelligentdiagnosis.control.infrastructure.audit;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_entries")
public class AuditEntryEntity {

    @Id
    private String id;

    private String traceId;

    private String userId;

    private String tenantId;

    @Enumerated(EnumType.STRING)
    private com.company.intelligentdiagnosis.control.domain.audit.AuditAction action;

    private String resource;

    private String resourceId;

    @Enumerated(EnumType.STRING)
    private com.company.intelligentdiagnosis.control.domain.audit.AuditResult result;

    private String reason;

    @Column(columnDefinition = "jsonb")
    private String context;

    private String ipAddress;

    private String userAgent;

    private Instant timestamp;

    private Instant completedAt;

    private String signature;

    public AuditEntryEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public com.company.intelligentdiagnosis.control.domain.audit.AuditAction getAction() {
        return action;
    }

    public void setAction(com.company.intelligentdiagnosis.control.domain.audit.AuditAction action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public com.company.intelligentdiagnosis.control.domain.audit.AuditResult getResult() {
        return result;
    }

    public void setResult(com.company.intelligentdiagnosis.control.domain.audit.AuditResult result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
