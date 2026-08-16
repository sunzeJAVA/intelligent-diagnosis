package com.company.intelligentdiagnosis.agent.infrastructure.llm;

import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * LLM client that iterates over multiple delegates and automatically falls back
 * to the next one when the current delegate throws an exception.
 */
public class CompositeLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(CompositeLlmClient.class);

    private final List<LlmClient> delegates;

    public CompositeLlmClient(List<LlmClient> delegates) {
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("At least one LLM client delegate is required");
        }
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public LlmCompletion complete(String systemPrompt, String userPrompt) {
        LlmClientException lastException = null;
        for (LlmClient delegate : delegates) {
            try {
                return delegate.complete(systemPrompt, userPrompt);
            } catch (Exception e) {
                log.warn("LLM delegate {} failed: {}", delegate.getClass().getSimpleName(), e.getMessage());
                lastException = new LlmClientException(
                    "LLM delegate failed: " + delegate.getClass().getSimpleName(), e);
            }
        }
        throw lastException != null ? lastException : new LlmClientException("All LLM delegates failed");
    }
}
