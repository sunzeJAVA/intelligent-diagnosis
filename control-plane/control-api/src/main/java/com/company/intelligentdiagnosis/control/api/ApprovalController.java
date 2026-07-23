package com.company.intelligentdiagnosis.control.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/control/approvals")
public class ApprovalController {

    @GetMapping
    public ResponseEntity<List<ApprovalDto>> listPending() {
        // TODO: 调用 application service
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/{workflowId}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable String workflowId,
            @RequestBody ApprovalRequest request) {
        // TODO: 发送 Temporal signal
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workflowId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable String workflowId,
            @RequestBody RejectionRequest request) {
        // TODO: 发送 Temporal signal
        return ResponseEntity.ok().build();
    }

    public record ApprovalDto(
        String workflowId,
        String repository,
        String commitHash,
        String riskLevel,
        String status
    ) {}

    public record ApprovalRequest(String approver, String comment) {}

    public record RejectionRequest(String reason) {}
}
