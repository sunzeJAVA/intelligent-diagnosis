package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import com.company.intelligentdiagnosis.agent.infrastructure.llm.LlmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingGenerator implements EmbeddingGenerator {

    private final RestClient restClient;
    private final EmbeddingProperties embeddingProperties;

    public OpenAiEmbeddingGenerator(LlmProperties llmProperties, EmbeddingProperties embeddingProperties) {
        this.restClient = RestClient.builder()
            .baseUrl(llmProperties.getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + llmProperties.getApiKey())
            .build();
        this.embeddingProperties = embeddingProperties;
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[dimension()];
        }

        var request = new EmbeddingRequest(text, embeddingProperties.getOpenai().getModel());
        EmbeddingResponse response = restClient.post()
            .uri("/embeddings")
            .body(request)
            .retrieve()
            .body(EmbeddingResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new VectorStoreException("Empty embedding response", null);
        }

        List<Double> embedding = response.data().get(0).embedding();
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i).floatValue();
        }
        return vector;
    }

    @Override
    public int dimension() {
        return embeddingProperties.getOpenai().getDimension();
    }

    public record EmbeddingRequest(String input, String model) {
    }

    public record EmbeddingResponse(List<Embedding> data) {
        public record Embedding(List<Double> embedding) {
        }
    }
}
