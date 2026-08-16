package com.company.intelligentdiagnosis.parse.java;

import com.company.intelligentdiagnosis.parse.HealthRequest;
import com.company.intelligentdiagnosis.parse.HealthResponse;
import com.company.intelligentdiagnosis.parse.ParseRequest;
import com.company.intelligentdiagnosis.parse.ParseResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JavaParseWorkerServiceTest {

    @Mock
    private JavaParserAnalyzer analyzer;

    @Mock
    private StreamObserver<ParseResponse> parseResponseObserver;

    @Mock
    private StreamObserver<HealthResponse> healthResponseObserver;

    @Test
    void shouldReturnHealthyOnHealthCheck() {
        JavaParseWorkerService service = new JavaParseWorkerService(analyzer);

        service.healthCheck(HealthRequest.getDefaultInstance(), healthResponseObserver);

        ArgumentCaptor<HealthResponse> captor = ArgumentCaptor.forClass(HealthResponse.class);
        verify(healthResponseObserver).onNext(captor.capture());
        verify(healthResponseObserver).onCompleted();

        HealthResponse response = captor.getValue();
        assertThat(response.getHealthy()).isTrue();
        assertThat(response.getVersion()).isEqualTo("0.1.0");
    }

    @Test
    void shouldDelegateParseToAnalyzer() {
        JavaParseWorkerService service = new JavaParseWorkerService(analyzer);
        ParseRequest request = ParseRequest.newBuilder()
            .setRepository("repo")
            .setCommitHash("abc123")
            .setRepoPath("/tmp/repo")
            .addAllChangedFiles(List.of("src/Main.java"))
            .build();

        when(analyzer.analyze(java.nio.file.Paths.get("/tmp/repo"), List.of("src/Main.java")))
            .thenReturn(List.of());

        service.parse(request, parseResponseObserver);

        ArgumentCaptor<ParseResponse> captor = ArgumentCaptor.forClass(ParseResponse.class);
        verify(parseResponseObserver).onNext(captor.capture());
        verify(parseResponseObserver).onCompleted();

        ParseResponse response = captor.getValue();
        assertThat(response.getErrorsList()).isEmpty();
        assertThat(response.getDurationMs()).isGreaterThanOrEqualTo(0);
    }
}
