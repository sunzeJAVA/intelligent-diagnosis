package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.IntentType;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.RewrittenQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedQueryRewriterTest {

    private final RuleBasedQueryRewriter rewriter = new RuleBasedQueryRewriter();

    @Test
    void shouldExpandAbbreviations() {
        DiagnosisIntent intent = DiagnosisIntent.unknown("");
        RewrittenQuery rewritten = rewriter.rewrite("NPE in UserService", intent);

        assertThat(rewritten.searchQuery()).containsIgnoringCase("NullPointerException");
        assertThat(rewritten.searchQuery()).contains("UserService");
        assertThat(rewritten.llmPromptQuery()).contains("UserService");
    }

    @Test
    void shouldAddIntentKeywords() {
        DiagnosisIntent intent = new DiagnosisIntent(IntentType.DATABASE_ERROR, 0.9, List.of("UserMapper"), "database error");
        RewrittenQuery rewritten = rewriter.rewrite("SQL timeout", intent);

        assertThat(rewritten.searchQuery()).containsIgnoringCase("database")
            .containsIgnoringCase("connection")
            .containsIgnoringCase("transaction");
    }

    @Test
    void shouldReturnIdentityForBlankInput() {
        RewrittenQuery rewritten = rewriter.rewrite("", DiagnosisIntent.unknown(""));
        assertThat(rewritten.searchQuery()).isEmpty();
        assertThat(rewritten.llmPromptQuery()).isEmpty();
    }

    @Test
    void shouldPreserveEntitiesInLlmPromptQuery() {
        DiagnosisIntent intent = new DiagnosisIntent(IntentType.NULL_POINTER, 0.8, List.of("OrderService"), "null pointer");
        RewrittenQuery rewritten = rewriter.rewrite("NullPointerException in OrderService", intent);

        assertThat(rewritten.llmPromptQuery()).contains("OrderService");
        assertThat(rewritten.llmPromptQuery()).contains("空指针异常");
    }

    @Test
    void shouldExpandChineseNullPointerSynonym() {
        DiagnosisIntent intent = DiagnosisIntent.unknown("");
        RewrittenQuery rewritten = rewriter.rewrite("空指针异常 in UserService", intent);

        assertThat(rewritten.searchQuery()).containsIgnoringCase("NullPointerException")
            .containsIgnoringCase("null object reference")
            .contains("UserService");
    }

    @Test
    void shouldExpandChineseDatabaseSynonym() {
        DiagnosisIntent intent = new DiagnosisIntent(IntentType.DATABASE_ERROR, 0.9, List.of("UserMapper"), "database error");
        RewrittenQuery rewritten = rewriter.rewrite("数据库连接超时", intent);

        assertThat(rewritten.searchQuery()).containsIgnoringCase("SQL")
            .containsIgnoringCase("database")
            .containsIgnoringCase("connection timeout");
    }

    @Test
    void shouldExpandMultipleChineseSynonyms() {
        DiagnosisIntent intent = new DiagnosisIntent(IntentType.CONCURRENCY, 0.8, List.of(), "");
        RewrittenQuery rewritten = rewriter.rewrite("并发死锁问题", intent);

        assertThat(rewritten.searchQuery()).containsIgnoringCase("concurrent")
            .containsIgnoringCase("deadlock")
            .containsIgnoringCase("thread")
            .containsIgnoringCase("race condition");
    }
}
