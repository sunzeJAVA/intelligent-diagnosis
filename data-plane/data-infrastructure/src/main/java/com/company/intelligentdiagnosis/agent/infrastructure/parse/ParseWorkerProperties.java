package com.company.intelligentdiagnosis.agent.infrastructure.parse;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 解析工作者配置属性
 * 配置不同语言对应的解析工作者端点
 */
@ConfigurationProperties(prefix = "parse.workers")
public record ParseWorkerProperties(
    /**
     * 语言到端点的映射
     */
    Map<String, Endpoint> endpoints
) {

    public ParseWorkerProperties {
        endpoints = endpoints == null ? Map.of() : endpoints;
    }

    /**
     * 获取指定语言的解析工作者端点
     *
     * @param language 编程语言
     * @return 端点配置，如果未配置则返回 null
     */
    public Endpoint endpointFor(String language) {
        return endpoints.get(language);
    }

    /**
     * 解析工作者端点配置
     */
    public record Endpoint(
        /**
         * 主机地址
         */
        String host,
        /**
         * 端口号
         */
        int port
    ) {
    }
}
