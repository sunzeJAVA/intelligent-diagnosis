package com.company.intelligentdiagnosis.agent.api;

/**
 * 工作流摘要 DTO
 * 从索引快照派生，用于工作流监控列表
 */
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
) {
}
