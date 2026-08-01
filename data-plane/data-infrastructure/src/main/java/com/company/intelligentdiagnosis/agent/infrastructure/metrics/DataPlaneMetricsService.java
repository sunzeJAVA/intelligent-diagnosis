package com.company.intelligentdiagnosis.agent.infrastructure.metrics;

import com.company.intelligentdiagnosis.agent.infrastructure.graph.GraphStoreClient;
import com.company.intelligentdiagnosis.agent.infrastructure.vector.QdrantProperties;
import io.micrometer.core.instrument.Counter;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

/**
 * 数据平面实时指标聚合服务
 */
@Service
public class DataPlaneMetricsService {

    private static final Logger log = LoggerFactory.getLogger(DataPlaneMetricsService.class);

    private final QdrantClient qdrantClient;
    private final QdrantProperties qdrantProperties;
    private final GraphStoreClient graphStoreClient;
    private final Counter diagnosisCounter;

    public DataPlaneMetricsService(QdrantClient qdrantClient,
                                   QdrantProperties qdrantProperties,
                                   GraphStoreClient graphStoreClient,
                                   Counter diagnosisCounter) {
        this.qdrantClient = qdrantClient;
        this.qdrantProperties = qdrantProperties;
        this.graphStoreClient = graphStoreClient;
        this.diagnosisCounter = diagnosisCounter;
    }

    /**
     * 聚合数据平面实时指标
     *
     * @return 指标数据
     */
    public MetricsDto collectMetrics() {
        long vectorCount = countQdrantPoints();
        long graphNodes = safeCount(graphStoreClient::countAllNodes, "Neo4j nodes");
        long graphRelations = safeCount(graphStoreClient::countAllRelations, "Neo4j relations");
        long diagnosisCount = (long) diagnosisCounter.count();

        return new MetricsDto(vectorCount, graphNodes, graphRelations, diagnosisCount);
    }

    private long countQdrantPoints() {
        try {
            return qdrantClient.countAsync(
                qdrantProperties.getCollectionName(),
                Points.Filter.getDefaultInstance(),
                true
            ).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while counting Qdrant points");
            return -1;
        } catch (ExecutionException e) {
            log.warn("Failed to count Qdrant points: {}", e.getCause().getMessage());
            return -1;
        }
    }

    private long safeCount(LongSupplier supplier, String label) {
        try {
            return supplier.getAsLong();
        } catch (Exception e) {
            log.warn("Failed to count {}: {}", label, e.getMessage());
            return -1;
        }
    }

    @FunctionalInterface
    interface LongSupplier {
        long getAsLong();
    }

    public record MetricsDto(
        long vectorCount,
        long graphNodes,
        long graphRelations,
        long diagnosisCount
    ) {}
}
