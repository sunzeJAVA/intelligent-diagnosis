package com.company.intelligentdiagnosis.control.api;

import com.company.intelligentdiagnosis.control.domain.audit.AuditEntry;
import com.company.intelligentdiagnosis.control.domain.audit.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/control/audits")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AuditDto>> listAudits(
        @RequestParam(required = false) String resource,
        @RequestParam(required = false) String userId
    ) {
        List<AuditEntry> entries;
        if (resource != null && !resource.isBlank()) {
            entries = auditService.getAuditsByResource(resource);
        } else if (userId != null && !userId.isBlank()) {
            entries = auditService.getAuditsByUser(userId);
        } else {
            entries = List.of();
        }
        return ResponseEntity.ok(entries.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/{auditId}/integrity")
    public ResponseEntity<IntegrityDto> verifyIntegrity(@PathVariable String auditId) {
        boolean valid = auditService.verifyIntegrity(auditId);
        return ResponseEntity.ok(new IntegrityDto(auditId, valid));
    }

    private AuditDto toDto(AuditEntry entry) {
        return new AuditDto(
            entry.id(),
            entry.action().name(),
            entry.resource(),
            entry.resourceId(),
            entry.result() != null ? entry.result().name() : null,
            entry.timestamp().toString(),
            entry.completedAt() != null ? entry.completedAt().toString() : null
        );
    }

    public record AuditDto(
        String id,
        String action,
        String resource,
        String resourceId,
        String result,
        String timestamp,
        String completedAt
    ) {}

    public record IntegrityDto(String auditId, boolean valid) {}
}
