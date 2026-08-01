package com.company.intelligentdiagnosis.agent.domain.workflow;

/**
 * 安全扫描器接口
 */
public interface SecurityScanner {

    /**
     * 扫描变更文件中的安全问题
     *
     * @param event Git 推送事件
     * @return 扫描结果
     */
    SecurityScanResult scan(GitPushEvent event);
}
