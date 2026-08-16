package com.company.intelligentdiagnosis.agent.domain.llm;

/**
 * LLM 调用结果
 * <p>
 * 封装模型返回的原始文本，并标记本次结果是否为降级响应（熔断、异常 fallback 等）。
 * 业务层可据此区分真实诊断与降级诊断，避免污染审计和指标。
 *
 * @param content  模型生成的响应文本
 * @param degraded true 表示本次为降级/兜底响应，非模型真实输出
 */
public record LlmCompletion(
    String content,
    boolean degraded
) {

    /**
     * 构造正常（非降级）的 LLM 调用结果
     */
    public static LlmCompletion normal(String content) {
        return new LlmCompletion(content, false);
    }

    /**
     * 构造降级响应结果
     */
    public static LlmCompletion degraded(String content) {
        return new LlmCompletion(content, true);
    }
}
