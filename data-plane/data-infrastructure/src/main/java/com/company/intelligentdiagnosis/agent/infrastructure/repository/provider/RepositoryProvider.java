package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryType;

import java.util.List;

/**
 * 仓库提供者接口
 * 定义仓库同步和变更检测的标准契约
 */
public interface RepositoryProvider {

    /**
     * 判断是否支持指定的仓库类型
     *
     * @param type 仓库类型
     * @return 如果支持则返回 true
     */
    boolean supports(RepositoryType type);

    /**
     * 同步仓库
     *
     * @param config 仓库配置
     * @return 同步结果
     * @throws RepositorySyncException 同步失败时抛出
     */
    RepositorySyncResult sync(RepositoryConfigEntity config) throws RepositorySyncException;

    /**
     * 检测两个提交之间的变更文件
     *
     * @param config     仓库配置
     * @param baseCommit 基准提交
     * @param headCommit 目标提交
     * @return 变更文件列表
     */
    List<String> detectChangedFiles(RepositoryConfigEntity config, String baseCommit, String headCommit);

    /**
     * 获取仓库最新提交哈希
     *
     * @param config 仓库配置
     * @return 最新提交哈希
     */
    String getLatestCommit(RepositoryConfigEntity config);
}
