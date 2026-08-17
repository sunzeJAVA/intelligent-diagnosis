package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeSnippet;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.IntentType;
import com.company.intelligentdiagnosis.agent.infrastructure.graph.GraphCodeElement;
import com.company.intelligentdiagnosis.agent.infrastructure.graph.GraphStoreClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphCodeRetrieverTest {

    @Mock
    private GraphStoreClient graphStoreClient;

    @Test
    void shouldReturnEmptyWhenNoService() {
        GraphCodeRetriever retriever = new GraphCodeRetriever(graphStoreClient);
        DiagnosisRequest request = new DiagnosisRequest("query", "error", "", "user", "tenant");

        List<CodeSnippet> snippets = retriever.retrieve(request);

        assertThat(snippets).isEmpty();
    }

    @Test
    void shouldRetrieveMatchedNodeAndCallChain() {
        GraphCodeRetriever retriever = new GraphCodeRetriever(graphStoreClient);
        DiagnosisRequest request = new DiagnosisRequest("UserService slow", "error", "svc", "user", "tenant");
        DiagnosisIntent intent = new DiagnosisIntent(IntentType.PERFORMANCE, 0.9, List.of("UserService"), "UserService slow");

        GraphCodeElement controllerNode = new GraphCodeElement(
            "controller-id", "METHOD", "getUserDetail", "UserController.getUserDetail",
            "UserController.java", 10, 20, "public void getUserDetail() {}", "",
            "获取用户详情", "Get user detail");
        GraphCodeElement serviceNode = new GraphCodeElement(
            "service-id", "METHOD", "getUserById", "UserService.getUserById",
            "UserService.java", 30, 40, "public User getUserById() {}", "",
            "根据 ID 获取用户", "Get user by id");

        when(graphStoreClient.findNodesByName(eq("svc"), anyString())).thenReturn(List.of(controllerNode));
        when(graphStoreClient.expandCallChain("svc", "controller-id", 2))
            .thenReturn(List.of(controllerNode, serviceNode));

        List<CodeSnippet> snippets = retriever.retrieve(request, intent);

        assertThat(snippets).hasSize(2);
        assertThat(snippets).extracting(CodeSnippet::filePath)
            .containsExactlyInAnyOrder("UserController.java", "UserService.java");
    }

    @Test
    void shouldExtractEntitiesFromQueryWhenIntentEntitiesEmpty() {
        GraphCodeRetriever retriever = new GraphCodeRetriever(graphStoreClient);
        DiagnosisRequest request = new DiagnosisRequest("OrderService timeout", "error", "svc", "user", "tenant");
        DiagnosisIntent intent = DiagnosisIntent.unknown("OrderService timeout");

        GraphCodeElement node = new GraphCodeElement(
            "order-id", "METHOD", "processOrder", "OrderService.processOrder",
            "OrderService.java", 5, 15, "public void processOrder() {}", "",
            "处理订单", "Process order");

        when(graphStoreClient.findNodesByName("svc", "OrderService")).thenReturn(List.of(node));
        when(graphStoreClient.expandCallChain("svc", "order-id", 2)).thenReturn(List.of(node));

        List<CodeSnippet> snippets = retriever.retrieve(request, intent);

        assertThat(snippets).hasSize(1);
        assertThat(snippets.get(0).filePath()).isEqualTo("OrderService.java");
    }
}
