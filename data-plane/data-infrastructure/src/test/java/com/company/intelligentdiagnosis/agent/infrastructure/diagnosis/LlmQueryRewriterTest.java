package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.IntentType;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.RewrittenQuery;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmCompletion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmQueryRewriterTest {

    @Mock
    private LlmClient llmClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseValidJsonResponse() {
        LlmQueryRewriter rewriter = new LlmQueryRewriter(llmClient, objectMapper);
        DiagnosisIntent intent = new DiagnosisIntent(IntentType.NULL_POINTER, 0.9, List.of("UserService"), "npe");

        when(llmClient.complete(anyString(), anyString())).thenReturn(LlmCompletion.normal("""
            {
              "searchQuery": "NullPointerException UserService null object reference",
              "llmPromptQuery": "NullPointerException occurred in UserService"
            }
            """));

        RewrittenQuery rewritten = rewriter.rewrite("NPE in UserService", intent);

        assertThat(rewritten.searchQuery()).isEqualTo("NullPointerException UserService null object reference");
        assertThat(rewritten.llmPromptQuery()).isEqualTo("NullPointerException occurred in UserService");
    }

    @Test
    void shouldParseFencedJsonResponse() {
        LlmQueryRewriter rewriter = new LlmQueryRewriter(llmClient, objectMapper);
        DiagnosisIntent intent = DiagnosisIntent.unknown("");

        when(llmClient.complete(anyString(), anyString())).thenReturn(LlmCompletion.normal("""
            ```json
            {
              "searchQuery": "database connection timeout",
              "llmPromptQuery": "Database connection timeout issue"
            }
            ```
            """));

        RewrittenQuery rewritten = rewriter.rewrite("DB timeout", intent);

        assertThat(rewritten.searchQuery()).isEqualTo("database connection timeout");
        assertThat(rewritten.llmPromptQuery()).isEqualTo("Database connection timeout issue");
    }

    @Test
    void shouldFallbackToOriginalWhenFieldsAreBlank() {
        LlmQueryRewriter rewriter = new LlmQueryRewriter(llmClient, objectMapper);
        DiagnosisIntent intent = DiagnosisIntent.unknown("");

        when(llmClient.complete(anyString(), anyString())).thenReturn(LlmCompletion.normal("""
            {
              "searchQuery": "",
              "llmPromptQuery": null
            }
            """));

        RewrittenQuery rewritten = rewriter.rewrite("original query", intent);

        assertThat(rewritten.searchQuery()).isEqualTo("original query");
        assertThat(rewritten.llmPromptQuery()).isEqualTo("original query");
    }

    @Test
    void shouldThrowExceptionForInvalidJson() {
        LlmQueryRewriter rewriter = new LlmQueryRewriter(llmClient, objectMapper);
        DiagnosisIntent intent = DiagnosisIntent.unknown("");

        when(llmClient.complete(anyString(), anyString())).thenReturn(LlmCompletion.normal("not json"));

        assertThatThrownBy(() -> rewriter.rewrite("query", intent))
            .isInstanceOf(QueryRewriteException.class)
            .hasMessageContaining("Failed to extract JSON");
    }

    @Test
    void shouldReturnIdentityForBlankOriginalQuery() {
        LlmQueryRewriter rewriter = new LlmQueryRewriter(llmClient, objectMapper);
        DiagnosisIntent intent = DiagnosisIntent.unknown("");

        RewrittenQuery rewritten = rewriter.rewrite("", intent);

        assertThat(rewritten.searchQuery()).isEmpty();
        assertThat(rewritten.llmPromptQuery()).isEmpty();
    }
}
