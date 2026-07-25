package com.company.intelligentdiagnosis.agent.domain.parse;

import java.util.List;

/**
 * 解析命令
 * 发送给解析工作器的解析任务指令
 *
 * @param repository  仓库名称
 * @param commitHash  提交哈希
 * @param repoPath    仓库本地路径
 * @param changedFiles 变更文件列表
 * @param language    编程语言
 */
public record ParseCommand(
    String repository,
    String commitHash,
    String repoPath,
    List<String> changedFiles,
    String language
) {
}
