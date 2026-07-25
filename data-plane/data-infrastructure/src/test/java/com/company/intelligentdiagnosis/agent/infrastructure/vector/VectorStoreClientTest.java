package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.ElementKind;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorStoreClientTest {

    @Mock
    private QdrantClient qdrantClient;

    @Mock
    private EmbeddingGenerator embeddingGenerator;

    @Mock
    private QdrantProperties properties;

    @InjectMocks
    private VectorStoreClient vectorStoreClient;

    @Test
    void shouldSkipUpsertForEmptyList() {
        vectorStoreClient.upsert("repo", List.of());

        verify(qdrantClient, never()).upsertAsync(anyString(), ArgumentMatchers.<List<Points.PointStruct>>any());
    }

    @Test
    void shouldUpsertIntoExistingCollection() {
        when(properties.getCollectionName()).thenReturn("code-elements");
        when(properties.isCreateCollectionIfMissing()).thenReturn(true);
        when(qdrantClient.collectionExistsAsync("code-elements")).thenReturn(immediateFuture(true));
        when(embeddingGenerator.embed(any())).thenReturn(new float[384]);
        when(qdrantClient.upsertAsync(anyString(), ArgumentMatchers.<List<Points.PointStruct>>any())).thenReturn(immediateFuture(null));

        vectorStoreClient.upsert("repo", List.of(codeElement()));

        verify(qdrantClient).collectionExistsAsync("code-elements");
        verify(qdrantClient).upsertAsync(eq("code-elements"), ArgumentMatchers.<List<Points.PointStruct>>any());
    }

    @Test
    void shouldCreateCollectionIfMissing() {
        when(properties.getCollectionName()).thenReturn("code-elements");
        when(properties.isCreateCollectionIfMissing()).thenReturn(true);
        when(qdrantClient.collectionExistsAsync("code-elements")).thenReturn(immediateFuture(false));
        when(embeddingGenerator.dimension()).thenReturn(384);
        when(qdrantClient.createCollectionAsync(anyString(), ArgumentMatchers.<Collections.VectorParams>any())).thenReturn(immediateFuture(null));
        when(embeddingGenerator.embed(any())).thenReturn(new float[384]);
        when(qdrantClient.upsertAsync(anyString(), ArgumentMatchers.<List<Points.PointStruct>>any())).thenReturn(immediateFuture(null));

        vectorStoreClient.upsert("repo", List.of(codeElement()));

        verify(qdrantClient).createCollectionAsync(eq("code-elements"), ArgumentMatchers.<Collections.VectorParams>any());
        verify(qdrantClient).upsertAsync(eq("code-elements"), ArgumentMatchers.<List<Points.PointStruct>>any());
    }

    @Test
    void shouldDeleteByRepository() {
        when(properties.getCollectionName()).thenReturn("code-elements");
        when(properties.isCreateCollectionIfMissing()).thenReturn(false);
        when(qdrantClient.deleteAsync(anyString(), any(Points.Filter.class))).thenReturn(immediateFuture(null));

        vectorStoreClient.deleteByRepository("repo");

        verify(qdrantClient).deleteAsync(eq("code-elements"), any(Points.Filter.class));
    }

    @Test
    void shouldSearchByRepository() {
        when(properties.getCollectionName()).thenReturn("code-elements");
        when(properties.isCreateCollectionIfMissing()).thenReturn(false);
        when(embeddingGenerator.embed("query")).thenReturn(new float[384]);
        Points.ScoredPoint point = Points.ScoredPoint.newBuilder().setScore(0.9f).build();
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class))).thenReturn(immediateFuture(List.of(point)));

        List<Points.ScoredPoint> results = vectorStoreClient.search("repo", "query", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getScore()).isCloseTo(0.9f, offset(0.0001f));
    }

    @Test
    void shouldWrapExecutionException() {
        when(properties.getCollectionName()).thenReturn("code-elements");
        when(properties.isCreateCollectionIfMissing()).thenReturn(true);
        when(qdrantClient.collectionExistsAsync("code-elements"))
            .thenReturn(Futures.immediateFailedFuture(new RuntimeException("boom")));

        assertThatThrownBy(() -> vectorStoreClient.upsert("repo", List.of(codeElement())))
            .isInstanceOf(VectorStoreException.class)
            .hasMessageContaining("Failed to create Qdrant collection");
    }

    private CodeElement codeElement() {
        return new CodeElement(
            "repo/src/Main.java#Main",
            ElementKind.CLASS,
            "Main",
            "Main",
            "src/Main.java",
            1,
            5,
            "class Main {}",
            "",
            List.of(),
            List.of(),
            java.util.Map.of()
        );
    }

    private static <T> ListenableFuture<T> immediateFuture(T value) {
        return Futures.immediateFuture(value);
    }
}
