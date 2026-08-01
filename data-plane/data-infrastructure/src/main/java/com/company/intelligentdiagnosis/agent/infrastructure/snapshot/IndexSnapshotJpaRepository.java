package com.company.intelligentdiagnosis.agent.infrastructure.snapshot;

import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexSnapshotJpaRepository extends JpaRepository<IndexSnapshotEntity, String> {

    List<IndexSnapshotEntity> findByRepositoryNameOrderByCreatedAtDesc(String repositoryName);

    List<IndexSnapshotEntity> findByStatusOrderByCreatedAtDesc(SnapshotStatus status);

    Optional<IndexSnapshotEntity> findFirstByRepositoryNameAndStatusOrderByCreatedAtDesc(
        String repositoryName, SnapshotStatus status
    );

    List<IndexSnapshotEntity> findAllByOrderByCreatedAtDesc();
}
