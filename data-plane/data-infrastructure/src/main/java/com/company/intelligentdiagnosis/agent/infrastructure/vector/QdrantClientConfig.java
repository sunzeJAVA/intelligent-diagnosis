package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QdrantProperties.class)
public class QdrantClientConfig {

    @Bean
    public QdrantClient qdrantClient(QdrantProperties properties) {
        return new QdrantClient(
            QdrantGrpcClient.newBuilder(properties.getHost(), properties.getPort(), false)
                .build()
        );
    }
}
