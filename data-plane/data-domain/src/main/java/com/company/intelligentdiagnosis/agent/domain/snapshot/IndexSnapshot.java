package com.company.intelligentdiagnosis.agent.domain.snapshot;

import java.time.Instant;
import java.util.List;

public record IndexSnapshot(
    String id,
    String repositoryId,
    String repositoryName,
    String branch,
    String commitHash,
    String previousCommit,
    String commitMessage,
    String author,
    SnapshotStatus status,
    long elementCount,
    long relationCount,
    String beforeSnapshotId,
    String afterSnapshotId,
    String checksum,
    String workflowId,
    Instant createdAt,
    Instant completedAt,
    List<String> elementIds,
    List<SnapshotValidation> validations,
    String qdrantSnapshotPath,
    String neo4jBackupPath
) {

    public IndexSnapshot withStatus(SnapshotStatus newStatus) {
        return new IndexSnapshot(
            id, repositoryId, repositoryName, branch, commitHash, previousCommit,
            commitMessage, author, newStatus, elementCount, relationCount,
            beforeSnapshotId, afterSnapshotId, checksum, workflowId,
            createdAt, completedAt, elementIds, validations,
            qdrantSnapshotPath, neo4jBackupPath
        );
    }

    public IndexSnapshot withValidations(List<SnapshotValidation> newValidations) {
        return new IndexSnapshot(
            id, repositoryId, repositoryName, branch, commitHash, previousCommit,
            commitMessage, author, status, elementCount, relationCount,
            beforeSnapshotId, afterSnapshotId, checksum, workflowId,
            createdAt, completedAt, elementIds, newValidations,
            qdrantSnapshotPath, neo4jBackupPath
        );
    }

    public IndexSnapshot withCompletedAt(Instant newCompletedAt) {
        return new IndexSnapshot(
            id, repositoryId, repositoryName, branch, commitHash, previousCommit,
            commitMessage, author, status, elementCount, relationCount,
            beforeSnapshotId, afterSnapshotId, checksum, workflowId,
            createdAt, newCompletedAt, elementIds, validations,
            qdrantSnapshotPath, neo4jBackupPath
        );
    }

    public IndexSnapshot withBackupPaths(String qdrantSnapshotPath, String neo4jBackupPath) {
        return new IndexSnapshot(
            id, repositoryId, repositoryName, branch, commitHash, previousCommit,
            commitMessage, author, status, elementCount, relationCount,
            beforeSnapshotId, afterSnapshotId, checksum, workflowId,
            createdAt, completedAt, elementIds, validations,
            qdrantSnapshotPath, neo4jBackupPath
        );
    }
}
