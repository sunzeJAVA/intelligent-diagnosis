package com.company.intelligentdiagnosis.agent.infrastructure.parse;

/**
 * Parse Worker 不可用异常
 * 当 gRPC 调用失败或熔断器开启时抛出
 */
public class ParseWorkerUnavailableException extends RuntimeException {

    public ParseWorkerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
