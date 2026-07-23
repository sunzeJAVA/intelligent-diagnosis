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

@Component
public class VectorStoreClient {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreClient.class);

    private final QdrantClient client;
    private final EmbeddingGenerator embeddingGenerator;
    private final QdrantProperties properties;

    public VectorStoreClient(QdrantClient client,
                             EmbeddingGenerator embeddingGenerator,
                             QdrantProperties properties) {
        this.client = client;
        this.embeddingGenerator = embeddingGenerator;
        this.properties = properties;
    }

    public void upsert(String repository, List<CodeElement> elements) {
        if (elements.isEmpty()) {
            return;
        }

        ensureCollection();

        List<Points.PointStruct> points = elements.stream()
            .map(element -> toPointStruct(repository, element))
            .toList();

        try {
            client.upsertAsync(properties.getCollectionName(), points).get();
            log.info("Upserted {} vectors to Qdrant collection {} for repository {}",
                points.size(), properties.getCollectionName(), repository);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while upserting vectors for repository: " + repository, e);
        } catch (ExecutionException e) {
            throw new VectorStoreException("Failed to upsert vectors for repository: " + repository, e.getCause());
        }
    }

    public void deleteByRepository(String repository) {
        ensureCollection();

        io.qdrant.client.grpc.Points.Filter filter = io.qdrant.client.grpc.Points.Filter.newBuilder()
            .addMust(matchKeyword("repository", repository))
            .build();

        try {
            client.deleteAsync(properties.getCollectionName(), filter).get();
            log.info("Deleted vectors for repository {} from Qdrant collection {}",
                repository, properties.getCollectionName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while deleting vectors for repository: " + repository, e);
        } catch (ExecutionException e) {
            throw new VectorStoreException("Failed to delete vectors for repository: " + repository, e.getCause());
        }
    }

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

    private void ensureCollection() {
        if (!properties.isCreateCollectionIfMissing()) {
            return;
        }

        try {
            boolean exists = client.collectionExistsAsync(properties.getCollectionName()).get();
            if (exists) {
                return;
            }
            client.createCollectionAsync(
                properties.getCollectionName(),
                Collections.VectorParams.newBuilder()
                    .setSize(embeddingGenerator.dimension())
                    .setDistance(Collections.Distance.Cosine)
                    .build()
            ).get();
            log.info("Created Qdrant collection {} with dimension {}",
                properties.getCollectionName(), embeddingGenerator.dimension());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Interrupted while creating Qdrant collection", e);
        } catch (ExecutionException e) {
            throw new VectorStoreException("Failed to create Qdrant collection", e.getCause());
        }
    }

    private Points.PointStruct toPointStruct(String repository, CodeElement element) {
        String text = buildEmbeddingText(element);
        float[] vector = embeddingGenerator.embed(text);

        return Points.PointStruct.newBuilder()
            .setId(id(pointIdFor(element.id())))
            .setVectors(vectors(vector))
            .putAllPayload(Map.of(
                "repository", value(repository),
                "kind", value(element.kind().name()),
                "name", value(element.name()),
                "qualifiedName", value(nullToEmpty(element.qualifiedName())),
                "filePath", value(nullToEmpty(element.filePath())),
                "startLine", value(element.startLine()),
                "endLine", value(element.endLine()),
                "sourceCode", value(nullToEmpty(element.sourceCode())),
                "documentation", value(nullToEmpty(element.documentation())),
                "text", value(text)
            ))
            .build();
    }

    private UUID pointIdFor(String elementId) {
        return UUID.nameUUIDFromBytes(elementId.getBytes(StandardCharsets.UTF_8));
    }

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

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }
}
