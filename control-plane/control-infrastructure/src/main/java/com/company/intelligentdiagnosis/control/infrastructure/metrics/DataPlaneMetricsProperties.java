package com.company.intelligentdiagnosis.control.infrastructure.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据平面指标客户端配置
 */
@ConfigurationProperties(prefix = "data-plane")
public class DataPlaneMetricsProperties {

    /**
     * 数据平面基础地址
     */
    private String baseUrl = "http://localhost:8082";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
