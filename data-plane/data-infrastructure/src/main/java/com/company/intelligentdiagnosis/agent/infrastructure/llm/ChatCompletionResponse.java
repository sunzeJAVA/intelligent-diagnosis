package com.company.intelligentdiagnosis.agent.infrastructure.llm;

import java.util.List;

public record ChatCompletionResponse(
    List<Choice> choices
) {
    public record Choice(
        int index,
        Message message,
        String finishReason
    ) {
    }

    public record Message(
        String role,
        String content
    ) {
    }
}
