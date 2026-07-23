package com.company.intelligentdiagnosis.agent.infrastructure.llm;

import java.util.List;

public record ChatCompletionRequest(
    String model,
    List<Message> messages,
    Double temperature,
    Integer maxTokens
) {
    public record Message(String role, String content) {
    }
}
