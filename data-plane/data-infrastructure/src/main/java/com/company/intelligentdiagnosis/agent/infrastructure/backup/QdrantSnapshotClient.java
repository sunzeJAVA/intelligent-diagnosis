package com.company.intelligentdiagnosis.agent.infrastructure.backup;

import com.company.intelligentdiagnosis.agent.infrastructure.vector.QdrantProperties;
import com.company.intelligentdiagnosis.agent.infrastructure.vector.VectorStoreException;
import com.google.protobuf.util.JsonFormat;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.RetrievedPoint;
import io.qdrant.client.grpc.Points.ScrollPoints;
import io.qdrant.client.grpc.Points.ScrollResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.qdrant.client.ConditionFactory.matchKeyword;

/**
 * Qdrant 向量索引物理快照客户端
 * <p>
 * 由于当前生产环境使用单一 collection 存放多个仓库的数据，而 Qdrant 的 snapshot API 是
 * 以 collection 为单位的，因此这里采用“按仓库滚动导出 point”的方式生成 point-level 快照，
 * 回滚时只影响目标仓库的数据。
 */
@Component
public class QdrantSnapshotClient {

    private static final Logger log = LoggerFactory.getLogger(QdrantSnapshotClient.class);
    private static final String SNAPSHOT_FILE = "qdrant-points.jsonl";
    private static final int SCROLL_LIMIT = 1000;

    private final QdrantClient client;
    private final QdrantProperties properties;
    private final BackupStorage backupStorage;

    public QdrantSnapshotClient(QdrantClient client,
                                QdrantProperties properties,
                                BackupStorage backupStorage) {
        this.client = client;
        this.properties = properties;
        this.backupStorage = backupStorage;
    }

    /**
     * 为指定仓库创建向量快照，返回本地备份文件绝对路径
     */
    public String createSnapshot(String repositoryName, String snapshotId) {
        String collectionName = properties.getCollectionName();
        List<PointStruct> points = scrollPoints(collectionName, repositoryName);

        String jsonl = points.stream()
            .map(this::toJson)
            .collect(Collectors.joining("\n"));

        Path path = backupStorage.writeString(repositoryName, snapshotId, SNAPSHOT_FILE, jsonl);
        log.info("Created Qdrant point-level snapshot for repository {} snapshot {}: {} points at {}",
            repositoryName, snapshotId, points.size(), path);
        return path.toAbsolutePath().toString();
    }

    /**
     * 从本地快照恢复指定仓库的向量数据
     */
    public void restoreSnapshot(String repositoryName, String snapshotId) {
        String collectionName = properties.getCollectionName();
        Path file = backupStorage.resolveSnapshotDir(repositoryName, snapshotId).resolve(SNAPSHOT_FILE);

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Qdrant snapshot file not found: " + file);
        }

        List<PointStruct> points;
        try (var lines = Files.lines(file)) {
            points = lines.filter(line -> !line.isBlank())
                .map(this::parsePoint)
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read Qdrant snapshot: " + file, e);
        }

        if (points.isEmpty()) {
            log.info("No Qdrant points to restore for repository {} snapshot {}", repositoryName, snapshotId);
            return;
        }

        try {
            client.upsertAsync(collectionName, points).get();
            log.info("Restored {} Qdrant points for repository {} snapshot {}",
                points.size(), repositoryName, snapshotId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while restoring Qdrant snapshot", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new VectorStoreException("Failed to restore Qdrant snapshot", e.getCause());
        }
    }

    private List<PointStruct> scrollPoints(String collectionName, String repositoryName) {
        List<PointStruct> result = new ArrayList<>();
        Points.PointId offset = null;

        Filter filter = Filter.newBuilder()
            .addMust(matchKeyword("repository", repositoryName))
            .build();

        try {
            do {
                ScrollPoints.Builder builder = ScrollPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .setFilter(filter)
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                    .setWithVectors(Points.WithVectorsSelector.newBuilder().setEnable(true).build())
                    .setLimit(SCROLL_LIMIT);
                if (offset != null) {
                    builder.setOffset(offset);
                }

                ScrollResponse response = client.scrollAsync(builder.build()).get();
                for (RetrievedPoint point : response.getResultList()) {
                    result.add(PointStruct.newBuilder()
                        .setId(point.getId())
                        .setVectors(point.getVectors())
                        .putAllPayload(point.getPayloadMap())
                        .build());
                }
                offset = response.hasNextPageOffset() ? response.getNextPageOffset() : null;
            } while (offset != null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while scrolling Qdrant points", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new VectorStoreException("Failed to scroll Qdrant points", e.getCause());
        }

        return result;
    }

    private String toJson(PointStruct point) {
        try {
            return JsonFormat.printer().print(point);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Qdrant point", e);
        }
    }

    private PointStruct parsePoint(String json) {
        try {
            PointStruct.Builder builder = PointStruct.newBuilder();
            JsonFormat.parser().merge(json, builder);
            return builder.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Qdrant point JSON: " + json, e);
        }
    }
}
