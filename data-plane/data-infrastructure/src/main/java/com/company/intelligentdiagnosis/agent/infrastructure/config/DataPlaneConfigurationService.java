package com.company.intelligentdiagnosis.agent.infrastructure.config;

import com.company.intelligentdiagnosis.agent.infrastructure.diagnosis.DiagnosisProperties;
import com.company.intelligentdiagnosis.agent.infrastructure.llm.LlmProperties;
import com.company.intelligentdiagnosis.agent.infrastructure.vector.EmbeddingProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据平面配置展示服务
 * <p>
 * 从各 Properties 类读取真实配置值，供控制平面聚合展示。
 * 输出的 {@link ConfigurationItem} JSON 结构需与控制平面
 * {@code SystemConfigurationService.ConfigurationItem} 保持一致
 * （字段：key, label, value, source）。
 */
@Service
public class DataPlaneConfigurationService {

    private final LlmProperties llmProperties;
    private final EmbeddingProperties embeddingProperties;
    private final DiagnosisProperties diagnosisProperties;

    public DataPlaneConfigurationService(LlmProperties llmProperties,
                                         EmbeddingProperties embeddingProperties,
                                         DiagnosisProperties diagnosisProperties) {
        this.llmProperties = llmProperties;
        this.embeddingProperties = embeddingProperties;
        this.diagnosisProperties = diagnosisProperties;
    }

    public List<ConfigurationItem> listConfigurations() {
        List<ConfigurationItem> result = new ArrayList<>();

        // LLM 配置
        result.add(new ConfigurationItem("llm.provider", "LLM 提供商",
            llmProperties.getProvider(), "data-plane"));
        result.add(new ConfigurationItem("llm.base-url", "LLM 基础 URL",
            llmProperties.getBaseUrl(), "data-plane"));
        result.add(new ConfigurationItem("llm.model", "LLM 模型",
            llmProperties.getModel(), "data-plane"));
        result.add(new ConfigurationItem("llm.max-tokens", "LLM 最大 Tokens",
            String.valueOf(llmProperties.getMaxTokens()), "data-plane"));
        result.add(new ConfigurationItem("llm.timeout-seconds", "LLM 超时 (秒)",
            llmProperties.getTimeoutSeconds() + "s", "data-plane"));

        // Embedding 配置
        result.add(new ConfigurationItem("embedding.provider", "Embedding 提供商",
            embeddingProperties.getProvider(), "data-plane"));
        result.add(new ConfigurationItem("embedding.openai.model", "Embedding 模型",
            embeddingProperties.getOpenai() != null ? embeddingProperties.getOpenai().getModel() : "—",
            "data-plane"));
        result.add(new ConfigurationItem("embedding.openai.dimension", "Embedding 维度",
            embeddingProperties.getOpenai() != null ? String.valueOf(embeddingProperties.getOpenai().getDimension()) : "—",
            "data-plane"));

        // 诊断配置
        result.add(new ConfigurationItem("diagnosis.top-k", "向量检索 TopK",
            String.valueOf(diagnosisProperties.getTopK()), "data-plane"));

        return result;
    }

    public record ConfigurationItem(
        String key,
        String label,
        String value,
        String source
    ) {}
}
