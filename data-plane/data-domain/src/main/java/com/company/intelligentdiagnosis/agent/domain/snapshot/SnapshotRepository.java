package com.company.intelligentdiagnosis.agent.domain.snapshot;

import java.util.List;
import java.util.Optional;

public interface SnapshotRepository {

    IndexSnapshot save(IndexSnapshot snapshot);

    Optional<IndexSnapshot> findById(String id);

    List<IndexSnapshot> findByRepositoryNameOrderByCreatedAtDesc(String repositoryName);

    List<IndexSnapshot> findByStatus(SnapshotStatus status);

    Optional<IndexSnapshot> findLatestPromotedByRepositoryName(String repositoryName);
}
