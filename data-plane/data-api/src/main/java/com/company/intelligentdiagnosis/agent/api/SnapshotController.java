package com.company.intelligentdiagnosis.agent.api;

import com.company.intelligentdiagnosis.agent.application.snapshot.SnapshotDiffService;
import com.company.intelligentdiagnosis.agent.application.snapshot.SnapshotRestoreService;
import com.company.intelligentdiagnosis.agent.domain.snapshot.IndexSnapshot;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data/snapshots")
public class SnapshotController {

    private final SnapshotRepository snapshotRepository;
    private final SnapshotDiffService snapshotDiffService;
    private final SnapshotRestoreService snapshotRestoreService;

    public SnapshotController(SnapshotRepository snapshotRepository,
                              SnapshotDiffService snapshotDiffService,
                              SnapshotRestoreService snapshotRestoreService) {
        this.snapshotRepository = snapshotRepository;
        this.snapshotDiffService = snapshotDiffService;
        this.snapshotRestoreService = snapshotRestoreService;
    }

    @GetMapping
    public ResponseEntity<List<SnapshotDto>> listSnapshots(@RequestParam String repository) {
        List<SnapshotDto> result = snapshotRepository.findByRepositoryNameOrderByCreatedAtDesc(repository).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SnapshotDto> getSnapshot(@PathVariable String id) {
        return snapshotRepository.findById(id)
            .map(snapshot -> ResponseEntity.ok(toDto(snapshot)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{leftId}/diff/{rightId}")
    public ResponseEntity<SnapshotDiffService.SnapshotDiff> diff(
        @PathVariable String leftId,
        @PathVariable String rightId
    ) {
        return ResponseEntity.ok(snapshotDiffService.diff(leftId, rightId));
    }

    @PostMapping("/{id}/rollback")
    public ResponseEntity<Void> rollback(@PathVariable String id) {
        snapshotRestoreService.restoreSnapshot(id);
        return ResponseEntity.ok().build();
    }

    private SnapshotDto toDto(IndexSnapshot snapshot) {
        return new SnapshotDto(
            snapshot.id(),
            snapshot.repositoryName(),
            snapshot.commitHash(),
            snapshot.status().name(),
            snapshot.elementCount(),
            snapshot.relationCount(),
            snapshot.beforeSnapshotId(),
            snapshot.afterSnapshotId(),
            snapshot.createdAt().toString(),
            snapshot.completedAt() != null ? snapshot.completedAt().toString() : null,
            snapshot.workflowId(),
            snapshot.qdrantSnapshotPath(),
            snapshot.neo4jBackupPath()
        );
    }

    public record SnapshotDto(
        String id,
        String repositoryName,
        String commitHash,
        String status,
        long elementCount,
        long relationCount,
        String beforeSnapshotId,
        String afterSnapshotId,
        String createdAt,
        String completedAt,
        String workflowId,
        String qdrantSnapshotPath,
        String neo4jBackupPath
    ) {}
}
