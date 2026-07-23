package com.company.intelligentdiagnosis.agent.infrastructure.parse;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParseWorkerPropertiesTest {

    @Test
    void shouldReturnEndpointForConfiguredLanguage() {
        ParseWorkerProperties.Endpoint java = new ParseWorkerProperties.Endpoint("localhost", 9093);
        ParseWorkerProperties properties = new ParseWorkerProperties(Map.of("java", java));

        assertThat(properties.endpointFor("java")).isEqualTo(java);
    }

    @Test
    void shouldReturnNullForMissingLanguage() {
        ParseWorkerProperties properties = new ParseWorkerProperties(Map.of("java",
            new ParseWorkerProperties.Endpoint("localhost", 9093)));

        assertThat(properties.endpointFor("csharp")).isNull();
    }

    @Test
    void shouldDefaultToEmptyEndpointsWhenNull() {
        ParseWorkerProperties properties = new ParseWorkerProperties(null);

        assertThat(properties.endpointFor("java")).isNull();
    }
}
