package com.company.intelligentdiagnosis.agent.infrastructure.snapshot;

import com.company.intelligentdiagnosis.agent.domain.snapshot.IndexSnapshot;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotRepository;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JpaSnapshotRepository implements SnapshotRepository {

    private final IndexSnapshotJpaRepository jpaRepository;

    public JpaSnapshotRepository(IndexSnapshotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public IndexSnapshot save(IndexSnapshot snapshot) {
        IndexSnapshotEntity entity = SnapshotMapper.toEntity(snapshot);
        IndexSnapshotEntity saved = jpaRepository.save(entity);
        return SnapshotMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IndexSnapshot> findById(String id) {
        return jpaRepository.findById(id).map(SnapshotMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndexSnapshot> findByRepositoryNameOrderByCreatedAtDesc(String repositoryName) {
        return jpaRepository.findByRepositoryNameOrderByCreatedAtDesc(repositoryName).stream()
            .map(SnapshotMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndexSnapshot> findByStatus(SnapshotStatus status) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(status).stream()
            .map(SnapshotMapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IndexSnapshot> findLatestPromotedByRepositoryName(String repositoryName) {
        return jpaRepository.findFirstByRepositoryNameAndStatusOrderByCreatedAtDesc(repositoryName, SnapshotStatus.PROMOTED)
            .map(SnapshotMapper::toDomain);
    }

    @Transactional(readOnly = true)
    Optional<IndexSnapshotEntity> findEntityById(String id) {
        return jpaRepository.findById(id);
    }

    @Transactional
    IndexSnapshotEntity saveEntity(IndexSnapshotEntity entity) {
        return jpaRepository.save(entity);
    }
}
