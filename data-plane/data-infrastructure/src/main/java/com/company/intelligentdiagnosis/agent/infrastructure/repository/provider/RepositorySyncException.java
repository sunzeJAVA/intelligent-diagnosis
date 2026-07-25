package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

/**
 * 仓库同步异常
 * 当仓库同步操作失败时抛出
 */
public class RepositorySyncException extends RuntimeException {

    /**
     * 创建异常实例
     *
     * @param message 异常消息
     */
    public RepositorySyncException(String message) {
        super(message);
    }

    /**
     * 创建异常实例，包含原因
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public RepositorySyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
