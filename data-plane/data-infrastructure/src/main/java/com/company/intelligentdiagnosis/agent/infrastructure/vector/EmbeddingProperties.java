package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 嵌入生成配置属性
 */
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {

    /**
     * 嵌入生成器提供商：token-hash 或 openai
     */
    private String provider = "token-hash";

    /**
     * OpenAI 相关配置
     */
    private final OpenAi openai = new OpenAi();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    /**
     * OpenAI 嵌入配置
     */
    public static class OpenAi {
        /**
         * 嵌入模型名称
         */
        private String model = "text-embedding-3-small";

        /**
         * 向量维度
         */
        private int dimension = 1536;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }
    }
}
