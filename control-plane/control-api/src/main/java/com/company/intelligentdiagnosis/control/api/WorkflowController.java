package com.company.intelligentdiagnosis.control.api;

import com.company.intelligentdiagnosis.control.domain.workflow.WorkflowInfo;
import com.company.intelligentdiagnosis.control.domain.workflow.WorkflowService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/control/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('workflow:read')")
    public ResponseEntity<List<WorkflowDto>> listWorkflows() {
        List<WorkflowDto> result = workflowService.listWorkflows().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{workflowId}")
    @PreAuthorize("hasAuthority('workflow:read')")
    public ResponseEntity<WorkflowDto> getWorkflow(@PathVariable String workflowId) {
        return ResponseEntity.ok(toDto(workflowService.getWorkflow(workflowId)));
    }

    @PostMapping("/{workflowId}/pause")
    @PreAuthorize("hasAuthority('workflow:write')")
    public ResponseEntity<Void> pause(@PathVariable String workflowId) {
        workflowService.pauseWorkflow(workflowId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workflowId}/resume")
    @PreAuthorize("hasAuthority('workflow:write')")
    public ResponseEntity<Void> resume(@PathVariable String workflowId) {
        workflowService.resumeWorkflow(workflowId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workflowId}/rollback")
    @PreAuthorize("hasAuthority('workflow:write')")
    public ResponseEntity<Void> rollback(@PathVariable String workflowId) {
        workflowService.rollbackWorkflow(workflowId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/start")
    @RateLimiter(name = "workflow-start")
    @PreAuthorize("hasAuthority('workflow:write')")
    public ResponseEntity<WorkflowStartResponse> startWorkflow(@RequestBody StartWorkflowRequest request) {
        String workflowId = workflowService.startIndexUpdateWorkflow(
            request.repositoryId(),
            request.repositoryName(),
            request.branch(),
            request.commitHash(),
            request.commitMessage(),
            request.author(),
            request.previousCommit(),
            request.changedFiles(),
            request.repoPath(),
            request.language(),
            request.triggeredBy()
        );
        return ResponseEntity.ok(new WorkflowStartResponse(workflowId));
    }

    private WorkflowDto toDto(WorkflowInfo info) {
        return new WorkflowDto(
            info.workflowId(),
            info.workflowType(),
            info.status(),
            info.currentStep(),
            info.startedAt() != null ? info.startedAt().toString() : null
        );
    }

    public record WorkflowDto(
        String workflowId,
        String workflowType,
        String status,
        String currentStep,
        String startedAt
    ) {}

    public record StartWorkflowRequest(
        String repositoryId,
        String repositoryName,
        String branch,
        String commitHash,
        String commitMessage,
        String author,
        String previousCommit,
        List<String> changedFiles,
        String repoPath,
        String language,
        String triggeredBy
    ) {}

    public record WorkflowStartResponse(String workflowId) {}
}
