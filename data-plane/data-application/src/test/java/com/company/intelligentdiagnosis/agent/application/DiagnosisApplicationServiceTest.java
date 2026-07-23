package com.company.intelligentdiagnosis.agent.application;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeRetriever;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeSnippet;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisAuditor;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisResponse;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.PolicyEngine;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisApplicationServiceTest {

    @Mock
    private CodeRetriever codeRetriever;

    @Mock
    private LlmClient llmClient;

    @Mock
    private DiagnosisAuditor auditor;

    @Mock
    private PolicyEngine policyEngine;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDiagnoseWithStructuredJsonResponse() {
        DiagnosisApplicationService service = new DiagnosisApplicationService(
            codeRetriever, llmClient, auditor, policyEngine, objectMapper
        );

        DiagnosisRequest request = new DiagnosisRequest("query", "error", "svc", "user", "tenant");
        CodeSnippet snippet = new CodeSnippet("src/Main.java", 1, 5, "class Main {}");
        when(codeRetriever.retrieve(request)).thenReturn(List.of(snippet));
        when(llmClient.complete(anyString(), anyString())).thenReturn("""
            {
              "summary": "summary",
              "rootCause": "root cause",
              "suggestions": ["fix 1"]
            }
            """);

        DiagnosisResponse response = service.diagnose(request);

        assertThat(response.summary()).isEqualTo("summary");
        assertThat(response.rootCause()).isEqualTo("root cause");
        assertThat(response.suggestions()).containsExactly("fix 1");
        assertThat(response.relatedCode()).containsExactly(snippet);

        ArgumentCaptor<DiagnosisResponse> responseCaptor = ArgumentCaptor.forClass(DiagnosisResponse.class);
        verify(auditor).record(eq(request), responseCaptor.capture(), anyLong());
        assertThat(responseCaptor.getValue().summary()).isEqualTo("summary");
    }

    @Test
    void shouldStripMarkdownFencesFromJsonResponse() {
        DiagnosisApplicationService service = new DiagnosisApplicationService(
            codeRetriever, llmClient, auditor, policyEngine, objectMapper
        );

        DiagnosisRequest request = new DiagnosisRequest("query", "error", "svc", "user", "tenant");
        when(codeRetriever.retrieve(request)).thenReturn(List.of());
        when(llmClient.complete(anyString(), anyString())).thenReturn("""
            ```json
            {
              "summary": "fenced summary",
              "rootCause": "fenced root cause",
              "suggestions": ["fenced fix"]
            }
            ```
            """);

        DiagnosisResponse response = service.diagnose(request);

        assertThat(response.summary()).isEqualTo("fenced summary");
        assertThat(response.rootCause()).isEqualTo("fenced root cause");
        assertThat(response.suggestions()).containsExactly("fenced fix");
    }

    @Test
    void shouldFallbackWhenLlmReturnsInvalidJson() {
        DiagnosisApplicationService service = new DiagnosisApplicationService(
            codeRetriever, llmClient, auditor, policyEngine, objectMapper
        );

        DiagnosisRequest request = new DiagnosisRequest("query", "error", "svc", "user", "tenant");
        when(codeRetriever.retrieve(request)).thenReturn(List.of());
        when(llmClient.complete(anyString(), anyString())).thenReturn("plain text");

        DiagnosisResponse response = service.diagnose(request);

        assertThat(response.summary()).isEqualTo("plain text");
        assertThat(response.rootCause()).isEmpty();
        assertThat(response.suggestions()).isEmpty();
    }
}
