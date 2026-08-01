package com.company.intelligentdiagnosis.control.infrastructure.workflow;

import com.company.intelligentdiagnosis.control.domain.workflow.WorkflowInfo;
import com.company.intelligentdiagnosis.control.domain.workflow.WorkflowService;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TemporalWorkflowServiceImpl implements WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(TemporalWorkflowServiceImpl.class);
    private static final String TASK_QUEUE = "index-update-task-queue";
    private static final String WORKFLOW_TYPE = "IndexUpdateWorkflow";

    @Autowired(required = false)
    private WorkflowClient workflowClient;

    private final DataPlaneWorkflowClient dataPlaneWorkflowClient;

    @Autowired
    public TemporalWorkflowServiceImpl(DataPlaneWorkflowClient dataPlaneWorkflowClient) {
        this.dataPlaneWorkflowClient = dataPlaneWorkflowClient;
    }

    private boolean isTemporalAvailable() {
        return workflowClient != null;
    }

    @Override
    public List<WorkflowInfo> listWorkflows() {
        if (dataPlaneWorkflowClient == null) {
            return List.of();
        }

        List<DataPlaneWorkflowClient.WorkflowSummaryDto> summaries = dataPlaneWorkflowClient.listWorkflows();
        if (summaries == null) {
            log.warn("Data-plane workflow list unavailable, returning empty list");
            return List.of();
        }

        return summaries.stream()
            .map(s -> new WorkflowInfo(
                s.workflowId(),
                s.workflowType(),
                s.status(),
                s.currentStep(),
                s.startedAt() != null ? Instant.parse(s.startedAt()) : null,
                s.completedAt() != null ? Instant.parse(s.completedAt()) : null,
                s.repositoryId(),
                s.repositoryName(),
                s.commitHash()
            ))
            .toList();
    }

    @Override
    public WorkflowInfo getWorkflow(String workflowId) {
        if (!isTemporalAvailable()) {
            log.warn("Temporal not available, returning fallback workflow info for: {}", workflowId);
            return fallbackWorkflowInfo(workflowId);
        }

        try {
            WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);
            String status = stub.query("getStatus", String.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stepHistory = stub.query("getStepHistory", List.class);
            String currentStep = stepHistory != null && !stepHistory.isEmpty()
                ? String.valueOf(stepHistory.get(stepHistory.size() - 1).get("name"))
                : null;

            return new WorkflowInfo(
                workflowId,
                WORKFLOW_TYPE,
                status,
                currentStep,
                Instant.now(),
                null,
                null,
                null,
                null
            );
        } catch (Exception e) {
            log.warn("Failed to query workflow {}: {}", workflowId, e.getMessage());
            return fallbackWorkflowInfo(workflowId);
        }
    }

    private WorkflowInfo fallbackWorkflowInfo(String workflowId) {
        return new WorkflowInfo(
            workflowId,
            WORKFLOW_TYPE,
            "UNKNOWN",
            null,
            Instant.now(),
            null,
            null,
            null,
            null
        );
    }

    @Override
    public void pauseWorkflow(String workflowId) {
        if (!isTemporalAvailable()) {
            log.warn("Temporal not available, skipping pause for workflow: {}", workflowId);
            return;
        }
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);
        stub.signal("pause", "Manual pause");
        log.info("Sent pause signal to workflow: {}", workflowId);
    }

    @Override
    public void resumeWorkflow(String workflowId) {
        if (!isTemporalAvailable()) {
            log.warn("Temporal not available, skipping resume for workflow: {}", workflowId);
            return;
        }
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);
        stub.signal("resume");
        log.info("Sent resume signal to workflow: {}", workflowId);
    }

    @Override
    public void rollbackWorkflow(String workflowId) {
        if (!isTemporalAvailable()) {
            log.warn("Temporal not available, skipping rollback for workflow: {}", workflowId);
            return;
        }
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);
        stub.signal("rollback");
        log.info("Sent rollback signal to workflow: {}", workflowId);
    }

    @Override
    public void approveWorkflow(String workflowId, String approver, String comment) {
        if (!isTemporalAvailable()) {
            log.warn("Temporal not available, skipping approve for workflow: {}", workflowId);
            return;
        }
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);
        stub.signal("approve", approver, comment);
        log.info("Sent approve signal to workflow {} by {}: {}", workflowId, approver, comment);
    }

    @Override
    public void rejectWorkflow(String workflowId, String reason) {
        if (!isTemporalAvailable()) {
            log.warn("Temporal not available, skipping reject for workflow: {}", workflowId);
            return;
        }
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);
        stub.signal("reject", reason);
        log.info("Sent reject signal to workflow {}: {}", workflowId, reason);
    }

    @Override
    public String startIndexUpdateWorkflow(String repositoryId, String repositoryName,
                                           String branch, String commitHash, String commitMessage,
                                           String author, String previousCommit, List<String> changedFiles,
                                           String repoPath, String language, String triggeredBy) {
        String workflowId = "index-update-" + repositoryId + "-" + UUID.randomUUID().toString().substring(0, 8);

        if (!isTemporalAvailable()) {
            log.warn("Temporal not available, workflow {} will not be started", workflowId);
            return workflowId;
        }

        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId(workflowId)
            .build();

        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(WORKFLOW_TYPE, options);
        stub.start("update", repositoryId, repositoryName, branch, commitHash, commitMessage,
            author, previousCommit, changedFiles, repoPath, language, triggeredBy);

        log.info("Started index update workflow: {} for repository {}", workflowId, repositoryName);
        return workflowId;
    }
}
