package com.company.intelligentdiagnosis.agent.infrastructure.repository.state;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositorySyncStateRepository extends JpaRepository<RepositorySyncStateEntity, String> {

    List<RepositorySyncStateEntity> findByRepositoryIdOrderByCreatedAtDesc(String repositoryId);

    Optional<RepositorySyncStateEntity> findFirstByRepositoryIdOrderByCreatedAtDesc(String repositoryId);
}
