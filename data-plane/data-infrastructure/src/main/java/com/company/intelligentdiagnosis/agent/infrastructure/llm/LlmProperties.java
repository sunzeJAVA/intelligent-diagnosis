package com.company.intelligentdiagnosis.agent.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 配置属性
 * 支持单模型配置和多模型配置两种方式
 */
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String provider = "mock";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private String apiKey = "";
    private int maxTokens = 2048;
    private int timeoutSeconds = 60;

    private List<ModelConfig> models = new ArrayList<>();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public List<ModelConfig> getModels() {
        return models;
    }

    public void setModels(List<ModelConfig> models) {
        this.models = models;
    }

    /**
     * 获取解析后的模型配置列表
     * 如果配置了多模型列表，则返回启用的模型列表；否则返回单模型配置
     *
     * @return 模型配置列表
     */
    public List<ModelConfig> resolvedModels() {
        if (models != null && !models.isEmpty()) {
            return models.stream().filter(ModelConfig::isEnabled).toList();
        }
        return List.of(new ModelConfig(provider, baseUrl, model, apiKey, maxTokens, timeoutSeconds, true));
    }

    /**
     * 模型配置
     */
    public static class ModelConfig {

        private String provider = "mock";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
        private String apiKey = "";
        private int maxTokens = 2048;
        private int timeoutSeconds = 60;
        private boolean enabled = true;

        public ModelConfig() {
        }

        public ModelConfig(String provider, String baseUrl, String model, String apiKey, int maxTokens, int timeoutSeconds, boolean enabled) {
            this.provider = provider;
            this.baseUrl = baseUrl;
            this.model = model;
            this.apiKey = apiKey;
            this.maxTokens = maxTokens;
            this.timeoutSeconds = timeoutSeconds;
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
