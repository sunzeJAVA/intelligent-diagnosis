package com.company.intelligentdiagnosis.agent.infrastructure.llm;

/**
 * LLM 客户端异常
 * 当调用 LLM 服务失败时抛出
 */
public class LlmClientException extends RuntimeException {

    /**
     * 创建异常实例
     *
     * @param message 异常消息
     */
    public LlmClientException(String message) {
        super(message);
    }

    /**
     * 创建异常实例，包含原因
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public LlmClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
