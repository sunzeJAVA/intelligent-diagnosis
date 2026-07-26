package com.company.intelligentdiagnosis.control.domain.audit;

import java.util.List;
import java.util.Map;

public interface AuditService {

    String startAudit(String action, String resource, String resourceId, Map<String, Object> context);

    void completeAudit(String auditId, AuditResult result, String reason);

    void completeAudit(String auditId, AuditResult result, Map<String, Object> details);

    void failAudit(String auditId, String reason);

    List<AuditEntry> getAuditsByResource(String resource);

    List<AuditEntry> getAuditsByUser(String userId);

    boolean verifyIntegrity(String auditId);
}
