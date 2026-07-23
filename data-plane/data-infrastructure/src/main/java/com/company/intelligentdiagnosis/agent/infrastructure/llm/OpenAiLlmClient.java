package com.company.intelligentdiagnosis.agent.infrastructure.llm;

import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

public class OpenAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private final RestClient restClient;
    private final LlmProperties properties;

    public OpenAiLlmClient(RestClient restClient, LlmProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        ChatCompletionRequest request = new ChatCompletionRequest(
            properties.getModel(),
            List.of(
                new ChatCompletionRequest.Message("system", systemPrompt),
                new ChatCompletionRequest.Message("user", userPrompt)
            ),
            0.2,
            properties.getMaxTokens()
        );

        log.info("Calling OpenAI-compatible LLM at {} with model {}", properties.getBaseUrl(), properties.getModel());

        ChatCompletionResponse response = restClient.post()
            .uri("/chat/completions")
            .body(request)
            .retrieve()
            .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new LlmClientException("Empty LLM response");
        }

        return response.choices().get(0).message().content();
    }
}
