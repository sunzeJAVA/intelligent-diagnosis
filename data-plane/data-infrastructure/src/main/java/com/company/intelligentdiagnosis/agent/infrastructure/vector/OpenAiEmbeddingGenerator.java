package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import com.company.intelligentdiagnosis.agent.infrastructure.llm.LlmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 基于 OpenAI API 的嵌入生成器
 * 调用 OpenAI 风格的嵌入 API 生成向量表示
 */
@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingGenerator implements EmbeddingGenerator {

    /**
     * REST 客户端
     */
    private final RestClient restClient;

    /**
     * 嵌入配置属性
     */
    private final EmbeddingProperties embeddingProperties;

    /**
     * 创建实例
     *
     * @param llmProperties       LLM 配置属性（用于获取 API Key 和 Base URL）
     * @param embeddingProperties 嵌入配置属性
     */
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

    /**
     * 嵌入请求
     */
    public record EmbeddingRequest(
        /**
         * 输入文本
         */
        String input,
        /**
         * 模型名称
         */
        String model
    ) {
    }

    /**
     * 嵌入响应
     */
    public record EmbeddingResponse(
        /**
         * 嵌入数据列表
         */
        List<Embedding> data
    ) {
        /**
         * 嵌入数据
         */
        public record Embedding(
            /**
             * 向量表示
             */
            List<Double> embedding
        ) {
        }
    }
}
