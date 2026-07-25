package com.company.intelligentdiagnosis.agent.infrastructure.vector;

/**
 * 向量存储异常
 * 当向量数据库操作失败时抛出
 */
public class VectorStoreException extends RuntimeException {

    /**
     * 创建异常实例
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
