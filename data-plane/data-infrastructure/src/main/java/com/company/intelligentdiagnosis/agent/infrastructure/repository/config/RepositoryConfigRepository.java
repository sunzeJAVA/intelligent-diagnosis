package com.company.intelligentdiagnosis.agent.infrastructure.repository.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryConfigRepository extends JpaRepository<RepositoryConfigEntity, String> {

    Optional<RepositoryConfigEntity> findByName(String name);

    List<RepositoryConfigEntity> findByEnabledTrue();

    boolean existsByName(String name);
}
