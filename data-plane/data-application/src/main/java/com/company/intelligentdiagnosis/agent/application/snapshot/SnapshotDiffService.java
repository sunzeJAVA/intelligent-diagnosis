package com.company.intelligentdiagnosis.agent.application.snapshot;

import com.company.intelligentdiagnosis.agent.domain.snapshot.IndexSnapshot;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SnapshotDiffService {

    private final SnapshotRepository snapshotRepository;

    public SnapshotDiffService(SnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public SnapshotDiff diff(String leftId, String rightId) {
        Optional<IndexSnapshot> leftOpt = snapshotRepository.findById(leftId);
        Optional<IndexSnapshot> rightOpt = snapshotRepository.findById(rightId);

        if (leftOpt.isEmpty()) {
            throw new IllegalArgumentException("Left snapshot not found: " + leftId);
        }
        if (rightOpt.isEmpty()) {
            throw new IllegalArgumentException("Right snapshot not found: " + rightId);
        }

        IndexSnapshot left = leftOpt.get();
        IndexSnapshot right = rightOpt.get();

        long elementDelta = right.elementCount() - left.elementCount();
        long relationDelta = right.relationCount() - left.relationCount();

        return new SnapshotDiff(
            leftId,
            rightId,
            left.repositoryName(),
            left.commitHash(),
            right.commitHash(),
            left.elementCount(),
            right.elementCount(),
            elementDelta,
            left.relationCount(),
            right.relationCount(),
            relationDelta,
            left.status().name(),
            right.status().name()
        );
    }

    public record SnapshotDiff(
        String leftSnapshotId,
        String rightSnapshotId,
        String repositoryName,
        String leftCommitHash,
        String rightCommitHash,
        long leftElementCount,
        long rightElementCount,
        long elementDelta,
        long leftRelationCount,
        long rightRelationCount,
        long relationDelta,
        String leftStatus,
        String rightStatus
    ) {}
}
