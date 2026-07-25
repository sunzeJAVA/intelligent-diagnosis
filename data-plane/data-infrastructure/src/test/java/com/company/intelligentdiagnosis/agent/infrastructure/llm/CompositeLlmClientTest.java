package com.company.intelligentdiagnosis.agent.infrastructure.llm;

import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeLlmClientTest {

    @Test
    void shouldReturnFirstSuccessfulResponse() {
        LlmClient first = mock(LlmClient.class);
        LlmClient second = mock(LlmClient.class);
        when(first.complete("system", "user")).thenReturn("first");
        when(second.complete("system", "user")).thenReturn("second");

        CompositeLlmClient client = new CompositeLlmClient(List.of(first, second));
        assertThat(client.complete("system", "user")).isEqualTo("first");
    }

    @Test
    void shouldFallbackWhenFirstFails() {
        LlmClient first = mock(LlmClient.class);
        LlmClient second = mock(LlmClient.class);
        when(first.complete("system", "user")).thenThrow(new LlmClientException("first failed"));
        when(second.complete("system", "user")).thenReturn("second");

        CompositeLlmClient client = new CompositeLlmClient(List.of(first, second));
        assertThat(client.complete("system", "user")).isEqualTo("second");
    }

    @Test
    void shouldThrowWhenAllDelegatesFail() {
        LlmClient first = mock(LlmClient.class);
        LlmClient second = mock(LlmClient.class);
        when(first.complete("system", "user")).thenThrow(new LlmClientException("first failed"));
        when(second.complete("system", "user")).thenThrow(new LlmClientException("second failed"));

        CompositeLlmClient client = new CompositeLlmClient(List.of(first, second));
        assertThatThrownBy(() -> client.complete("system", "user"))
            .isInstanceOf(LlmClientException.class)
            .hasMessageContaining("LLM delegate failed");
    }

    @Test
    void shouldRejectEmptyDelegates() {
        assertThatThrownBy(() -> new CompositeLlmClient(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one LLM client delegate is required");
    }
}
