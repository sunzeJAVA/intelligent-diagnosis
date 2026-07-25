package com.company.intelligentdiagnosis.agent.infrastructure.llm;

import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * LLM 客户端配置类
 * 根据配置属性动态创建 LLM 客户端，支持多模型自动降级策略
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmClientConfig {

    /**
     * 创建 LLM 客户端 Bean
     * 根据配置的模型列表创建对应的客户端实例，支持单模型和多模型模式
     *
     * @param properties LLM 配置属性
     * @return LLM 客户端实例
     */
    @Bean
    public LlmClient llmClient(LlmProperties properties) {
        List<LlmProperties.ModelConfig> models = properties.resolvedModels();
        if (models.isEmpty()) {
            return new MockDiagnosisLlmClient();
        }
        List<LlmClient> clients = models.stream().map(this::createClient).toList();
        if (clients.size() == 1) {
            return clients.get(0);
        }
        return new CompositeLlmClient(clients);
    }

    /**
     * 根据模型配置创建对应的 LLM 客户端
     *
     * @param model 模型配置
     * @return LLM 客户端实例
     */
    private LlmClient createClient(LlmProperties.ModelConfig model) {
        return switch (model.getProvider().toLowerCase()) {
            case "mock" -> new MockDiagnosisLlmClient();
            case "openai" -> createOpenAiClient(model);
            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + model.getProvider());
        };
    }

    /**
     * 创建 OpenAI 风格的 LLM 客户端
     *
     * @param model 模型配置
     * @return OpenAI 客户端实例
     */
    private LlmClient createOpenAiClient(LlmProperties.ModelConfig model) {
        if (model.getApiKey() == null || model.getApiKey().isBlank()) {
            throw new IllegalStateException(
                "LLM provider is openai but api-key is not configured for model " + model.getModel());
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(model.getTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(model.getTimeoutSeconds()));

        RestClient restClient = RestClient.builder()
            .baseUrl(model.getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + model.getApiKey())
            .requestFactory(factory)
            .build();
        return new OpenAiLlmClient(restClient, toProperties(model));
    }

    /**
     * 将 ModelConfig 转换为 LlmProperties
     *
     * @param model 模型配置
     * @return LLM 属性对象
     */
    private LlmProperties toProperties(LlmProperties.ModelConfig model) {
        LlmProperties properties = new LlmProperties();
        properties.setProvider(model.getProvider());
        properties.setBaseUrl(model.getBaseUrl());
        properties.setModel(model.getModel());
        properties.setApiKey(model.getApiKey());
        properties.setMaxTokens(model.getMaxTokens());
        properties.setTimeoutSeconds(model.getTimeoutSeconds());
        return properties;
    }
}
