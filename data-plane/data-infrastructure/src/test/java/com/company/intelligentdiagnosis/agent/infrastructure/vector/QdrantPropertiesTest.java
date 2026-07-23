package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantPropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        QdrantProperties properties = new QdrantProperties();

        assertThat(properties.getHost()).isEqualTo("localhost");
        assertThat(properties.getPort()).isEqualTo(6334);
        assertThat(properties.getCollectionName()).isEqualTo("code-elements");
        assertThat(properties.isCreateCollectionIfMissing()).isTrue();
    }

    @Test
    void shouldAllowCustomValues() {
        QdrantProperties properties = new QdrantProperties();
        properties.setHost("qdrant");
        properties.setPort(1234);
        properties.setCollectionName("custom");
        properties.setCreateCollectionIfMissing(false);

        assertThat(properties.getHost()).isEqualTo("qdrant");
        assertThat(properties.getPort()).isEqualTo(1234);
        assertThat(properties.getCollectionName()).isEqualTo("custom");
        assertThat(properties.isCreateCollectionIfMissing()).isFalse();
    }
}
