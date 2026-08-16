package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeSnippet;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridCodeRetrieverTest {

    @Mock
    private QdrantCodeRetriever vectorCodeRetriever;

    @Mock
    private GraphCodeRetriever graphCodeRetriever;

    @Test
    void shouldMergeAndDeduplicateResults() {
        DiagnosisProperties properties = new DiagnosisProperties();
        properties.setTopK(10);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        HybridCodeRetriever hybrid = new HybridCodeRetriever(vectorCodeRetriever, graphCodeRetriever, properties, meterRegistry);

        DiagnosisRequest request = new DiagnosisRequest("query", "error", "svc", "user", "tenant");
        DiagnosisIntent intent = DiagnosisIntent.unknown("query");

        CodeSnippet vectorSnippet = new CodeSnippet("UserController.java", 1, 5, "class UserController {}");
        CodeSnippet graphSnippet = new CodeSnippet("UserService.java", 10, 20, "class UserService {}");
        CodeSnippet duplicateSnippet = new CodeSnippet("UserController.java", 1, 5, "duplicate");

        when(vectorCodeRetriever.retrieve(any(), any())).thenReturn(List.of(vectorSnippet, duplicateSnippet));
        when(graphCodeRetriever.retrieve(any(), any())).thenReturn(List.of(graphSnippet, duplicateSnippet));

        List<CodeSnippet> result = hybrid.retrieve(request, intent);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CodeSnippet::filePath)
            .containsExactly("UserController.java", "UserService.java");
    }

    @Test
    void shouldRespectTopKLimit() {
        DiagnosisProperties properties = new DiagnosisProperties();
        properties.setTopK(2);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        HybridCodeRetriever hybrid = new HybridCodeRetriever(vectorCodeRetriever, graphCodeRetriever, properties, meterRegistry);

        DiagnosisRequest request = new DiagnosisRequest("query", "error", "svc", "user", "tenant");
        DiagnosisIntent intent = DiagnosisIntent.unknown("query");

        CodeSnippet s1 = new CodeSnippet("A.java", 1, 2, "a");
        CodeSnippet s2 = new CodeSnippet("B.java", 3, 4, "b");
        CodeSnippet s3 = new CodeSnippet("C.java", 5, 6, "c");

        when(vectorCodeRetriever.retrieve(any(), any())).thenReturn(List.of(s1));
        when(graphCodeRetriever.retrieve(any(), any())).thenReturn(List.of(s2, s3));

        List<CodeSnippet> result = hybrid.retrieve(request, intent);

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldFallbackWhenVectorRetrieverFails() {
        DiagnosisProperties properties = new DiagnosisProperties();
        properties.setTopK(10);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        HybridCodeRetriever hybrid = new HybridCodeRetriever(vectorCodeRetriever, graphCodeRetriever, properties, meterRegistry);

        DiagnosisRequest request = new DiagnosisRequest("query", "error", "svc", "user", "tenant");
        DiagnosisIntent intent = DiagnosisIntent.unknown("query");

        CodeSnippet graphSnippet = new CodeSnippet("UserService.java", 10, 20, "class UserService {}");

        when(vectorCodeRetriever.retrieve(any(), any())).thenThrow(new RuntimeException("vector failed"));
        when(graphCodeRetriever.retrieve(any(), any())).thenReturn(List.of(graphSnippet));

        List<CodeSnippet> result = hybrid.retrieve(request, intent);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).filePath()).isEqualTo("UserService.java");
        assertThat(meterRegistry.find("diagnosis.retrieval.failure.total").counter())
            .isNotNull()
            .satisfies(c -> assertThat(c.count()).isEqualTo(1.0));
    }
}
