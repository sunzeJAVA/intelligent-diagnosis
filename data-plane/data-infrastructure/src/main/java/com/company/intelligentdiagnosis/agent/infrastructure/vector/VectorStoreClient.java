package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * Qdrant 向量存储客户端
 * 负责代码元素的向量索引、查询和管理
 */
@Component
public class VectorStoreClient {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreClient.class);

    private final QdrantClient client;
    private final EmbeddingGenerator embeddingGenerator;
    private final QdrantProperties properties;

    /**
     * 构造函数
     *
     * @param client              Qdrant 客户端
     * @param embeddingGenerator  嵌入向量生成器
     * @param properties          Qdrant 配置属性
     */
    public VectorStoreClient(QdrantClient client,
                             EmbeddingGenerator embeddingGenerator,
                             QdrantProperties properties) {
        this.client = client;
        this.embeddingGenerator = embeddingGenerator;
        this.properties = properties;
    }

    /**
     * 插入或更新向量到生产集合
     *
     * @param repository 仓库名称
     * @param elements   代码元素列表
     */
    public void upsert(String repository, List<CodeElement> elements) {
        ensureCollection();
        upsert(properties.getCollectionName(), repository, elements);
    }

    /**
     * 插入或更新向量到沙箱集合
     *
     * @param repository 仓库名称
     * @param elements   代码元素列表
     */
    public void upsertToSandbox(String repository, List<CodeElement> elements) {
        ensureSandboxCollection();
        upsert(properties.getSandboxCollectionName(), repository, elements);
    }

    private void upsert(String collectionName, String repository, List<CodeElement> elements) {
        if (elements.isEmpty()) {
            return;
        }

        List<Points.PointStruct> points = elements.stream()
            .map(element -> toPointStruct(repository, element))
            .toList();

        try {
            client.upsertAsync(collectionName, points).get();
            log.info("Upserted {} vectors to Qdrant collection {} for repository {}",
                points.size(), collectionName, repository);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while upserting vectors for repository: " + repository, e);
        } catch (ExecutionException e) {
            throw new VectorStoreException("Failed to upsert vectors for repository: " + repository, e.getCause());
        }
    }

    /**
     * 将沙箱集合中指定仓库的向量提升到生产集合
     *
     * @param repository 仓库名称
     */
    public void promoteSandboxToProduction(String repository) {
        ensureCollection();
        ensureSandboxCollection();

        try {
            Points.ScrollPoints scroll = Points.ScrollPoints.newBuilder()
                .setCollectionName(properties.getSandboxCollectionName())
                .setFilter(io.qdrant.client.grpc.Points.Filter.newBuilder()
                    .addMust(matchKeyword("repository", repository))
                    .build())
                .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                .setWithVectors(Points.WithVectorsSelector.newBuilder().setEnable(true).build())
                .setLimit(1000)
                .build();

            Points.ScrollResponse response = client.scrollAsync(scroll).get();
            List<Points.PointStruct> points = response.getResultList().stream()
                .map(point -> Points.PointStruct.newBuilder()
                    .setId(point.getId())
                    .setVectors(point.getVectors())
                    .putAllPayload(point.getPayloadMap())
                    .build())
                .toList();

            if (!points.isEmpty()) {
                client.upsertAsync(properties.getCollectionName(), points).get();
            }

            deleteByRepository(properties.getSandboxCollectionName(), repository);
            log.info("Promoted {} vectors from sandbox to production for repository {}", points.size(), repository);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while promoting sandbox vectors for repository: " + repository, e);
        } catch (ExecutionException e) {
            throw new VectorStoreException("Failed to promote sandbox vectors for repository: " + repository, e.getCause());
        }
    }

    /**
     * 删除生产集合中指定仓库的所有向量
     *
     * @param repository 仓库名称
     */
    public void deleteByRepository(String repository) {
        ensureCollection();
        deleteByRepository(properties.getCollectionName(), repository);
    }

    private void deleteByRepository(String collectionName, String repository) {
        io.qdrant.client.grpc.Points.Filter filter = io.qdrant.client.grpc.Points.Filter.newBuilder()
            .addMust(matchKeyword("repository", repository))
            .build();

        try {
            client.deleteAsync(collectionName, filter).get();
            log.info("Deleted vectors for repository {} from Qdrant collection {}",
                repository, collectionName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while deleting vectors for repository: " + repository, e);
        } catch (ExecutionException e) {
            throw new VectorStoreException("Failed to delete vectors for repository: " + repository, e.getCause());
        }
    }

    /**
     * 统计指定集合中某仓库的向量数量
     *
     * @param collectionName 集合名称
     * @param repository     仓库名称
     * @return 向量数量
     */
    public long countByRepository(String collectionName, String repository) {
        io.qdrant.client.grpc.Points.Filter filter = io.qdrant.client.grpc.Points.Filter.newBuilder()
            .addMust(matchKeyword("repository", repository))
            .build();

        try {
            return client.countAsync(collectionName, filter, true).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while counting vectors for repository: " + repository, e);
        } catch (ExecutionException e) {
            throw new VectorStoreException("Failed to count vectors for repository: " + repository, e.getCause());
        }
    }

    /**
     * 向量相似度搜索
     * 根据查询文本检索相关的代码元素
     *
     * @param repository 仓库名称
     * @param queryText  查询文本
     * @param topK       返回结果数量
     * @return 带分数的向量点列表
     */
    public List<ScoredPoint> search(String repository, String queryText, int topK) {
        ensureCollection();

        float[] vector = embeddingGenerator.embed(queryText);

        Filter filter = Filter.newBuilder()
            .addMust(matchKeyword("repository", repository))
            .build();

        SearchPoints search = SearchPoints.newBuilder()
            .setCollectionName(properties.getCollectionName())
            .addAllVector(toFloatList(vector))
            .setFilter(filter)
            .setLimit(topK)
            .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
            .build();

        try {
            List<ScoredPoint> results = client.searchAsync(search).get();
            log.info("Vector search for repository {} returned {} results", repository, results.size());
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while searching vectors for repository: " + repository, e);
        } catch (ExecutionException e) {
            throw new VectorStoreException("Failed to search vectors for repository: " + repository, e.getCause());
        }
    }

    /**
     * 确保集合存在
     * 如果配置允许且集合不存在，则创建集合
     */
    private void ensureCollection() {
        ensureCollection(properties.getCollectionName());
    }

    private void ensureSandboxCollection() {
        ensureCollection(properties.getSandboxCollectionName());
    }

    private void ensureCollection(String collectionName) {
        if (!properties.isCreateCollectionIfMissing()) {
            return;
        }

        try {
            boolean exists = client.collectionExistsAsync(collectionName).get();
            if (exists) {
                return;
            }
            client.createCollectionAsync(
                collectionName,
                Collections.VectorParams.newBuilder()
                    .setSize(embeddingGenerator.dimension())
                    .setDistance(Collections.Distance.Cosine)
                    .build()
            ).get();
            log.info("Created Qdrant collection {} with dimension {}",
                collectionName, embeddingGenerator.dimension());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while creating Qdrant collection", e);
        } catch (ExecutionException e) {
            throw new VectorStoreException("Failed to create Qdrant collection", e.getCause());
        }
    }

    /**
     * 将代码元素转换为 Qdrant 点结构
     *
     * @param repository 仓库名称
     * @param element    代码元素
     * @return 点结构
     */
    private Points.PointStruct toPointStruct(String repository, CodeElement element) {
        String text = buildEmbeddingText(element);
        float[] vector = embeddingGenerator.embed(text);

        Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new java.util.HashMap<>();
        payload.put("repository", value(repository));
        payload.put("kind", value(element.kind().name()));
        payload.put("name", value(element.name()));
        payload.put("qualifiedName", value(nullToEmpty(element.qualifiedName())));
        payload.put("filePath", value(nullToEmpty(element.filePath())));
        payload.put("startLine", value(element.startLine()));
        payload.put("endLine", value(element.endLine()));
        payload.put("sourceCode", value(nullToEmpty(element.sourceCode())));
        payload.put("documentation", value(nullToEmpty(element.documentation())));
        payload.put("chineseSummary", value(nullToEmpty(element.chineseSummary())));
        payload.put("englishSummary", value(nullToEmpty(element.englishSummary())));
        payload.put("text", value(text));

        return Points.PointStruct.newBuilder()
            .setId(id(pointIdFor(element.id())))
            .setVectors(vectors(vector))
            .putAllPayload(payload)
            .build();
    }

    /**
     * 根据元素 ID 生成点 ID
     *
     * @param elementId 元素 ID
     * @return UUID 点 ID
     */
    private UUID pointIdFor(String elementId) {
        return UUID.nameUUIDFromBytes(elementId.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 构建嵌入文本
     * 将元素的全限定名、文档和源代码组合成用于嵌入的文本
     *
     * @param element 代码元素
     * @return 嵌入文本
     */
    private String buildEmbeddingText(CodeElement element) {
        StringBuilder builder = new StringBuilder();
        if (element.qualifiedName() != null && !element.qualifiedName().isBlank()) {
            builder.append(element.qualifiedName()).append("\n");
        }
        if (element.documentation() != null && !element.documentation().isBlank()) {
            builder.append(element.documentation()).append("\n");
        }
        if (element.sourceCode() != null && !element.sourceCode().isBlank()) {
            builder.append(element.sourceCode());
        }
        return builder.toString().trim();
    }

    /**
     * 空字符串处理
     *
     * @param value 字符串值
     * @return 非空字符串
     */
    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * 浮点数组转换为列表
     *
     * @param vector 浮点数组
     * @return 浮点列表
     */
    private List<Float> toFloatList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }
}
