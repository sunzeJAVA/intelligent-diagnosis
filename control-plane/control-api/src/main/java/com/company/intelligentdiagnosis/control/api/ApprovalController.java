package com.company.intelligentdiagnosis.control.api;

import com.company.intelligentdiagnosis.control.application.ApprovalApplicationService;
import com.company.intelligentdiagnosis.control.domain.workflow.WorkflowInfo;
import com.company.intelligentdiagnosis.control.domain.workflow.WorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/control/approvals")
public class ApprovalController {

    private final ApprovalApplicationService approvalApplicationService;
    private final WorkflowService workflowService;

    public ApprovalController(ApprovalApplicationService approvalApplicationService,
                             WorkflowService workflowService) {
        this.approvalApplicationService = approvalApplicationService;
        this.workflowService = workflowService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('approval:read')")
    public ResponseEntity<List<ApprovalDto>> listPending() {
        List<ApprovalDto> result = workflowService.listWorkflows().stream()
            .filter(w -> "AWAITING_APPROVAL".equals(w.status()))
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{workflowId}/approve")
    @PreAuthorize("hasAuthority('approval:write')")
    public ResponseEntity<Void> approve(
            @PathVariable String workflowId,
            @RequestBody ApprovalRequest request) {
        approvalApplicationService.approve(workflowId, request.approver(), request.comment());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workflowId}/reject")
    @PreAuthorize("hasAuthority('approval:write')")
    public ResponseEntity<Void> reject(
            @PathVariable String workflowId,
            @RequestBody RejectionRequest request) {
        approvalApplicationService.reject(workflowId, request.reason());
        return ResponseEntity.ok().build();
    }

    private ApprovalDto toDto(WorkflowInfo info) {
        return new ApprovalDto(
            info.workflowId(),
            info.repositoryName(),
            info.commitHash(),
            null,
            info.status()
        );
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
