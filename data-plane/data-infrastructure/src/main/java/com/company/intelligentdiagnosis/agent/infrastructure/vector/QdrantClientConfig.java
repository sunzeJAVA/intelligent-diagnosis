package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant 客户端配置类
 */
@Configuration
@EnableConfigurationProperties(QdrantProperties.class)
public class QdrantClientConfig {

    /**
     * 创建 Qdrant 客户端 Bean
     *
     * @param properties Qdrant 配置属性
     * @return Qdrant 客户端实例
     */
    @Bean
    public QdrantClient qdrantClient(QdrantProperties properties) {
        return new QdrantClient(
            QdrantGrpcClient.newBuilder(properties.getHost(), properties.getPort(), false)
                .build()
        );
    }
}
