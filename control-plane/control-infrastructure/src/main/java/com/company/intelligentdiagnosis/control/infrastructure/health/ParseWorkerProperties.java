package com.company.intelligentdiagnosis.control.infrastructure.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Parse Worker 端点配置（控制平面侧）
 * <p>
 * 仅用于健康检查的 socket 探测，与数据平面的 {@code parse.workers} 配置保持一致。
 */
@ConfigurationProperties(prefix = "parse.workers")
public class ParseWorkerProperties {

    /**
     * 语言到端点的映射
     */
    private Map<String, Endpoint> endpoints = Map.of();

    public Map<String, Endpoint> getEndpoints() { return endpoints; }
    public void setEndpoints(Map<String, Endpoint> endpoints) { this.endpoints = endpoints; }

    public record Endpoint(String host, int port, String language) {}
}
