package com.company.intelligentdiagnosis.control.infrastructure.workflow;

import com.company.intelligentdiagnosis.control.infrastructure.metrics.DataPlaneMetricsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * 数据平面工作流客户端
 */
@Component
public class DataPlaneWorkflowClient {

    private static final Logger log = LoggerFactory.getLogger(DataPlaneWorkflowClient.class);

    private final RestClient restClient;

    public DataPlaneWorkflowClient(DataPlaneMetricsProperties properties) {
        this.restClient = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .build();
    }

    /**
     * 从数据平面获取工作流列表
     *
     * @return 工作流摘要列表，若调用失败返回 null
     */
    public List<WorkflowSummaryDto> listWorkflows() {
        try {
            return restClient.get()
                .uri("/api/data/workflows")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            log.warn("Failed to fetch workflows from data-plane: {}", e.getMessage());
            return null;
        }
    }

    public record WorkflowSummaryDto(
        String workflowId,
        String workflowType,
        String status,
        String currentStep,
        String startedAt,
        String completedAt,
        String repositoryId,
        String repositoryName,
        String commitHash
    ) {}
}
