package com.company.intelligentdiagnosis.agent.infrastructure.enrichment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 代码元素富化配置属性
 */
@ConfigurationProperties(prefix = "enrichment")
public class EnrichmentProperties {

    /**
     * 是否启用富化
     */
    private boolean enabled = true;

    /**
     * 每次 LLM 调用处理的元素数量
     */
    private int batchSize = 1;

    /**
     * 富化超时时间（秒）
     */
    private int timeoutSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
