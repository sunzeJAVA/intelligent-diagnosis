package com.company.intelligentdiagnosis.agent.domain.llm;

public interface LlmClient {

    String complete(String systemPrompt, String userPrompt);
}
