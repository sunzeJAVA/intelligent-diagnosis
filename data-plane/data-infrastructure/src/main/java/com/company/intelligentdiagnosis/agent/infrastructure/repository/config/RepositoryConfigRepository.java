package com.company.intelligentdiagnosis.agent.infrastructure.repository.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 仓库配置数据访问接口
 */
@Repository
public interface RepositoryConfigRepository extends JpaRepository<RepositoryConfigEntity, String> {

    /**
     * 根据名称查找仓库配置
     *
     * @param name 仓库名称
     * @return 仓库配置
     */
    Optional<RepositoryConfigEntity> findByName(String name);

    /**
     * 查找所有启用的仓库配置
     *
     * @return 启用的仓库配置列表
     */
    List<RepositoryConfigEntity> findByEnabledTrue();

    /**
     * 检查名称是否已存在
     *
     * @param name 仓库名称
     * @return 如果存在则返回 true
     */
    boolean existsByName(String name);
}
