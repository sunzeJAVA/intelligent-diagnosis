package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.RewrittenQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositeQueryRewriterTest {

    @Mock
    private LlmQueryRewriter llmQueryRewriter;

    @Mock
    private RuleBasedQueryRewriter ruleBasedQueryRewriter;

    @Test
    void shouldUseLlmResultWhenSuccessful() {
        CompositeQueryRewriter composite = new CompositeQueryRewriter(llmQueryRewriter, ruleBasedQueryRewriter);
        DiagnosisIntent intent = DiagnosisIntent.unknown("query");
        RewrittenQuery expected = new RewrittenQuery("llm search", "llm prompt");

        when(llmQueryRewriter.rewrite("query", intent)).thenReturn(expected);

        RewrittenQuery rewritten = composite.rewrite("query", intent);

        assertThat(rewritten).isEqualTo(expected);
    }

    @Test
    void shouldFallbackToRuleBasedWhenLlmFails() {
        CompositeQueryRewriter composite = new CompositeQueryRewriter(llmQueryRewriter, ruleBasedQueryRewriter);
        DiagnosisIntent intent = DiagnosisIntent.unknown("query");
        RewrittenQuery expected = new RewrittenQuery("rule search", "rule prompt");

        when(llmQueryRewriter.rewrite(any(), any())).thenThrow(new RuntimeException("LLM failed"));
        when(ruleBasedQueryRewriter.rewrite("query", intent)).thenReturn(expected);

        RewrittenQuery rewritten = composite.rewrite("query", intent);

        assertThat(rewritten).isEqualTo(expected);
    }

    @Test
    void shouldReturnIdentityForBlankInput() {
        CompositeQueryRewriter composite = new CompositeQueryRewriter(llmQueryRewriter, ruleBasedQueryRewriter);
        DiagnosisIntent intent = DiagnosisIntent.unknown("");

        RewrittenQuery rewritten = composite.rewrite("", intent);

        assertThat(rewritten.searchQuery()).isEmpty();
        assertThat(rewritten.llmPromptQuery()).isEmpty();
    }
}
