package com.company.intelligentdiagnosis.agent.api;

import com.company.intelligentdiagnosis.agent.domain.snapshot.IndexSnapshot;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据平面工作流 API
 * 基于索引快照表派生工作流列表
 */
@RestController
@RequestMapping("/api/data/workflows")
public class WorkflowController {

    private static final String WORKFLOW_TYPE = "IndexUpdateWorkflow";

    private final SnapshotRepository snapshotRepository;

    public WorkflowController(SnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('workflow:read')")
    public ResponseEntity<List<WorkflowSummaryDto>> listWorkflows() {
        List<WorkflowSummaryDto> result = snapshotRepository.findAllOrderByCreatedAtDesc().stream()
            .filter(snapshot -> snapshot.workflowId() != null && !snapshot.workflowId().isBlank())
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private WorkflowSummaryDto toDto(IndexSnapshot snapshot) {
        return new WorkflowSummaryDto(
            snapshot.workflowId(),
            WORKFLOW_TYPE,
            snapshot.status().name(),
            null,
            snapshot.createdAt() != null ? snapshot.createdAt().toString() : null,
            snapshot.completedAt() != null ? snapshot.completedAt().toString() : null,
            snapshot.repositoryId(),
            snapshot.repositoryName(),
            snapshot.commitHash()
        );
    }
}
