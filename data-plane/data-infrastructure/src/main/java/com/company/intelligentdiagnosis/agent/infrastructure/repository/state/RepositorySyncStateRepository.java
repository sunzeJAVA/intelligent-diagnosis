package com.company.intelligentdiagnosis.agent.infrastructure.repository.state;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 仓库同步状态数据访问接口
 */
@Repository
public interface RepositorySyncStateRepository extends JpaRepository<RepositorySyncStateEntity, String> {

    /**
     * 根据仓库 ID 查找同步状态，按创建时间倒序排列
     *
     * @param repositoryId 仓库 ID
     * @return 同步状态列表
     */
    List<RepositorySyncStateEntity> findByRepositoryIdOrderByCreatedAtDesc(String repositoryId);

    /**
     * 查找仓库最新的同步状态
     *
     * @param repositoryId 仓库 ID
     * @return 最新同步状态
     */
    Optional<RepositorySyncStateEntity> findFirstByRepositoryIdOrderByCreatedAtDesc(String repositoryId);
}
