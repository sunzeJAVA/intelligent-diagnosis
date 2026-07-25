package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

import java.util.List;

/**
 * 仓库同步结果
 */
public record RepositorySyncResult(
    /**
     * 最新提交哈希
     */
    String latestCommit,
    /**
     * 同步前的提交哈希
     */
    String previousCommit,
    /**
     * 变更文件列表
     */
    List<String> changedFiles,
    /**
     * 是否为首次克隆
     */
    boolean freshClone
) {
}
