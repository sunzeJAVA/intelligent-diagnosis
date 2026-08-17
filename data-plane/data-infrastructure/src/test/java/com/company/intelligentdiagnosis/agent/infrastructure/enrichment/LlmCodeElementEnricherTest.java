package com.company.intelligentdiagnosis.agent.infrastructure.enrichment;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.ElementKind;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmCompletion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmCodeElementEnricherTest {

    @Mock
    private LlmClient llmClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EnrichmentProperties properties = new EnrichmentProperties();

    @Test
    void shouldEnrichElementWithSummaries() {
        LlmCodeElementEnricher enricher = new LlmCodeElementEnricher(llmClient, objectMapper, properties);
        CodeElement element = createElement();

        when(llmClient.complete(anyString(), anyString()))
            .thenReturn(LlmCompletion.normal("""
                {"chineseSummary": "获取用户详情", "englishSummary": "Get user detail"}
                """));

        List<CodeElement> result = enricher.enrich(List.of(element));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).chineseSummary()).isEqualTo("获取用户详情");
        assertThat(result.get(0).englishSummary()).isEqualTo("Get user detail");
    }

    @Test
    void shouldReturnOriginalWhenDisabled() {
        properties.setEnabled(false);
        LlmCodeElementEnricher enricher = new LlmCodeElementEnricher(llmClient, objectMapper, properties);
        CodeElement element = createElement();

        List<CodeElement> result = enricher.enrich(List.of(element));

        assertThat(result).containsExactly(element);
    }

    @Test
    void shouldReturnOriginalOnDegradedResponse() {
        LlmCodeElementEnricher enricher = new LlmCodeElementEnricher(llmClient, objectMapper, properties);
        CodeElement element = createElement();

        when(llmClient.complete(anyString(), anyString()))
            .thenReturn(LlmCompletion.degraded("服务不可用"));

        List<CodeElement> result = enricher.enrich(List.of(element));

        assertThat(result).containsExactly(element);
    }

    @Test
    void shouldReturnOriginalWhenJsonInvalid() {
        LlmCodeElementEnricher enricher = new LlmCodeElementEnricher(llmClient, objectMapper, properties);
        CodeElement element = createElement();

        when(llmClient.complete(anyString(), anyString()))
            .thenReturn(LlmCompletion.normal("not json"));

        List<CodeElement> result = enricher.enrich(List.of(element));

        assertThat(result).containsExactly(element);
    }

    @Test
    void shouldHandleEmptyList() {
        LlmCodeElementEnricher enricher = new LlmCodeElementEnricher(llmClient, objectMapper, properties);

        List<CodeElement> result = enricher.enrich(List.of());

        assertThat(result).isEmpty();
    }

    private CodeElement createElement() {
        return new CodeElement(
            "repo/UserService.java#getUserById",
            ElementKind.METHOD,
            "getUserById",
            "UserService.getUserById",
            "UserService.java",
            10,
            20,
            "public User getUserById(Long id) { return userMapper.findById(id); }",
            "",
            List.of("public"),
            List.of(),
            java.util.Map.of(),
            null,
            null
        );
    }
}
