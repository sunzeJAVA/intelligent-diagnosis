package com.company.intelligentdiagnosis.agent.infrastructure.parse;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "parse.workers")
public record ParseWorkerProperties(Map<String, Endpoint> endpoints) {

    public ParseWorkerProperties {
        endpoints = endpoints == null ? Map.of() : endpoints;
    }

    public Endpoint endpointFor(String language) {
        return endpoints.get(language);
    }

    public record Endpoint(String host, int port) {
    }
}
