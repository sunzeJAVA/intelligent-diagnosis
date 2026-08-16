package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

/**
 * Query 重写失败时抛出的异常
 * <p>
 * 用于把 LLM 重写失败的信息向上传播，方便外层（如 {@link CompositeQueryRewriter}）决策是否降级。
 */
public class QueryRewriteException extends RuntimeException {

    public QueryRewriteException(String message) {
        super(message);
    }

    public QueryRewriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
