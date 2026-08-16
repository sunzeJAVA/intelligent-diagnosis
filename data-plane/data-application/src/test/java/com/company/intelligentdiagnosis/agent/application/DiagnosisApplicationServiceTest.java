package com.company.intelligentdiagnosis.agent.application;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeRetriever;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeSnippet;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisAuditor;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisResponse;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.IntentRecognizer;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.IntentType;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.PolicyEngine;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.QueryRewriter;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.RewrittenQuery;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmCompletion;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
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

    @Mock
    private IntentRecognizer intentRecognizer;

    @Mock
    private QueryRewriter queryRewriter;

    @Mock
    private Counter diagnosisCounter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDiagnoseWithStructuredJsonResponse() {
        DiagnosisApplicationService service = new DiagnosisApplicationService(
            codeRetriever, llmClient, auditor, policyEngine, intentRecognizer, queryRewriter, objectMapper, diagnosisCounter
        );

        DiagnosisRequest request = new DiagnosisRequest("query", "error", "svc", "user", "tenant");
        // 意图识别返回 unknown 且 enhancedQuery 等于原 query，确保 retrieve 接收原始 request
        DiagnosisIntent intent = DiagnosisIntent.unknown("query");
        when(intentRecognizer.recognize(request)).thenReturn(intent);
        when(queryRewriter.rewrite(eq("query"), any(DiagnosisIntent.class))).thenReturn(RewrittenQuery.identity("query"));
        CodeSnippet snippet = new CodeSnippet("src/Main.java", 1, 5, "class Main {}");
        when(codeRetriever.retrieve(eq(request), any(DiagnosisIntent.class))).thenReturn(List.of(snippet));
        when(llmClient.complete(anyString(), anyString())).thenReturn(LlmCompletion.normal("""
            {
              "summary": "summary",
              "rootCause": "root cause",
              "suggestions": ["fix 1"]
            }
            """));

        DiagnosisResponse response = service.diagnose(request);

        assertThat(response.summary()).isEqualTo("summary");
        assertThat(response.rootCause()).isEqualTo("root cause");
        assertThat(response.suggestions()).containsExactly("fix 1");
        assertThat(response.relatedCode()).containsExactly(snippet);
        assertThat(response.intent()).isNotNull();
        assertThat(response.intent().type()).isEqualTo(IntentType.UNKNOWN);
        assertThat(response.degraded()).isFalse();
        verify(diagnosisCounter).increment();

        ArgumentCaptor<DiagnosisResponse> responseCaptor = ArgumentCaptor.forClass(DiagnosisResponse.class);
        verify(auditor).record(eq(request), responseCaptor.capture(), anyLong());
        assertThat(responseCaptor.getValue().summary()).isEqualTo("summary");
        assertThat(responseCaptor.getValue().degraded()).isFalse();
    }

    @Test
    void shouldStripMarkdownFencesFromJsonResponse() {
        DiagnosisApplicationService service = new DiagnosisApplicationService(
            codeRetriever, llmClient, auditor, policyEngine, intentRecognizer, queryRewriter, objectMapper, diagnosisCounter
        );

        DiagnosisRequest request = new DiagnosisRequest("query", "error", "svc", "user", "tenant");
        when(intentRecognizer.recognize(request)).thenReturn(DiagnosisIntent.unknown("query"));
        when(queryRewriter.rewrite(eq("query"), any(DiagnosisIntent.class))).thenReturn(RewrittenQuery.identity("query"));
        when(codeRetriever.retrieve(eq(request), any(DiagnosisIntent.class))).thenReturn(List.of());
        when(llmClient.complete(anyString(), anyString())).thenReturn(LlmCompletion.normal("""
            ```json
            {
              "summary": "fenced summary",
              "rootCause": "fenced root cause",
              "suggestions": ["fenced fix"]
            }
            ```
            """));

        DiagnosisResponse response = service.diagnose(request);

        assertThat(response.summary()).isEqualTo("fenced summary");
        assertThat(response.rootCause()).isEqualTo("fenced root cause");
        assertThat(response.suggestions()).containsExactly("fenced fix");
    }

    @Test
    void shouldFallbackWhenLlmReturnsInvalidJson() {
        DiagnosisApplicationService service = new DiagnosisApplicationService(
            codeRetriever, llmClient, auditor, policyEngine, intentRecognizer, queryRewriter, objectMapper, diagnosisCounter
        );

        DiagnosisRequest request = new DiagnosisRequest("query", "error", "svc", "user", "tenant");
        when(intentRecognizer.recognize(request)).thenReturn(DiagnosisIntent.unknown("query"));
        when(queryRewriter.rewrite(eq("query"), any(DiagnosisIntent.class))).thenReturn(RewrittenQuery.identity("query"));
        when(codeRetriever.retrieve(eq(request), any(DiagnosisIntent.class))).thenReturn(List.of());
        when(llmClient.complete(anyString(), anyString())).thenReturn(LlmCompletion.normal("plain text"));

        DiagnosisResponse response = service.diagnose(request);

        assertThat(response.summary()).isEqualTo("plain text");
        assertThat(response.rootCause()).isEmpty();
        assertThat(response.suggestions()).isEmpty();
        assertThat(response.degraded()).isTrue();
        verify(diagnosisCounter, Mockito.never()).increment();
    }
}
