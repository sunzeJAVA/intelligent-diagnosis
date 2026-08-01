package com.company.intelligentdiagnosis.agent.infrastructure.snapshot;

import com.company.intelligentdiagnosis.agent.domain.snapshot.IndexSnapshot;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SnapshotMapper {

    private SnapshotMapper() {
    }

    public static IndexSnapshotEntity toEntity(IndexSnapshot snapshot) {
        IndexSnapshotEntity entity = new IndexSnapshotEntity();
        entity.setId(snapshot.id());
        entity.setRepositoryId(snapshot.repositoryId());
        entity.setRepositoryName(snapshot.repositoryName());
        entity.setBranch(snapshot.branch());
        entity.setCommitHash(snapshot.commitHash());
        entity.setPreviousCommit(snapshot.previousCommit());
        entity.setCommitMessage(snapshot.commitMessage());
        entity.setAuthor(snapshot.author());
        entity.setStatus(snapshot.status());
        entity.setElementCount(snapshot.elementCount());
        entity.setRelationCount(snapshot.relationCount());
        entity.setBeforeSnapshotId(snapshot.beforeSnapshotId());
        entity.setAfterSnapshotId(snapshot.afterSnapshotId());
        entity.setChecksum(snapshot.checksum());
        entity.setWorkflowId(snapshot.workflowId());
        entity.setElementIds(snapshot.elementIds() != null ? snapshot.elementIds() : List.of());
        entity.setCreatedAt(snapshot.createdAt());
        entity.setCompletedAt(snapshot.completedAt());
        entity.setQdrantSnapshotPath(snapshot.qdrantSnapshotPath());
        entity.setNeo4jBackupPath(snapshot.neo4jBackupPath());

        if (snapshot.validations() != null) {
            entity.setValidations(snapshot.validations().stream()
                .map(v -> toEntity(v, entity))
                .collect(Collectors.toList()));
        }

        return entity;
    }

    public static SnapshotValidationEntity toEntity(SnapshotValidation validation, IndexSnapshotEntity snapshot) {
        SnapshotValidationEntity entity = new SnapshotValidationEntity();
        entity.setId(validation.id() != null ? validation.id() : UUID.randomUUID().toString());
        entity.setSnapshot(snapshot);
        entity.setValidationType(validation.type());
        entity.setResult(validation.result());
        entity.setDetails(validation.details());
        entity.setValidatedAt(validation.validatedAt());
        return entity;
    }

    public static IndexSnapshot toDomain(IndexSnapshotEntity entity) {
        return new IndexSnapshot(
            entity.getId(),
            entity.getRepositoryId(),
            entity.getRepositoryName(),
            entity.getBranch(),
            entity.getCommitHash(),
            entity.getPreviousCommit(),
            entity.getCommitMessage(),
            entity.getAuthor(),
            entity.getStatus(),
            entity.getElementCount(),
            entity.getRelationCount(),
            entity.getBeforeSnapshotId(),
            entity.getAfterSnapshotId(),
            entity.getChecksum(),
            entity.getWorkflowId(),
            entity.getCreatedAt(),
            entity.getCompletedAt(),
            entity.getElementIds() != null ? entity.getElementIds() : List.of(),
            entity.getValidations() != null
                ? entity.getValidations().stream().map(SnapshotMapper::toDomain).collect(Collectors.toList())
                : new ArrayList<>(),
            entity.getQdrantSnapshotPath(),
            entity.getNeo4jBackupPath()
        );
    }

    public static SnapshotValidation toDomain(SnapshotValidationEntity entity) {
        return new SnapshotValidation(
            entity.getId(),
            entity.getSnapshot().getId(),
            entity.getValidationType(),
            entity.getResult(),
            entity.getDetails(),
            entity.getValidatedAt()
        );
    }
}
