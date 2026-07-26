package com.company.intelligentdiagnosis.agent.infrastructure.workflow;

import com.company.intelligentdiagnosis.agent.domain.workflow.*;
import com.company.intelligentdiagnosis.agent.domain.workflow.activity.IndexUpdateActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class IndexUpdateWorkflowImpl implements IndexUpdateWorkflow {

    private final IndexUpdateActivities activities = Workflow.newActivityStub(
        IndexUpdateActivities.class,
        ActivityOptions.newBuilder()
            .setScheduleToCloseTimeout(Duration.ofMinutes(30))
            .setStartToCloseTimeout(Duration.ofMinutes(10))
            .build()
    );

    private UpdateStatus status = UpdateStatus.PENDING;
    private List<WorkflowStep> steps = new ArrayList<>();
    private boolean approved = false;
    private String rejectionReason;
    private boolean paused = false;
    private String pauseReason;
    private boolean rollbackRequested = false;
    private String beforeSnapshotId;

    @Override
    public UpdateResult update(GitPushEvent event) {
        status = UpdateStatus.RUNNING;

        try {
            executeStep("DETECT_CHANGES", () -> {
                activities.detectChanges(event);
                return null;
            });

            SecurityScanResult scanResult = executeStep("SECURITY_SCAN", () -> activities.scanSecurity(event));
            if (!scanResult.isPassed()) {
                return UpdateResult.failed("Security scan failed: " + scanResult.getReason(), steps);
            }

            RiskLevel risk = executeStep("CLASSIFY_RISK", () -> activities.classifyRisk(event));

            if (risk == RiskLevel.HIGH) {
                status = UpdateStatus.AWAITING_APPROVAL;
                Workflow.await(() -> approved || rejectionReason != null);

                if (rejectionReason != null) {
                    status = UpdateStatus.REJECTED;
                    return UpdateResult.rejected(rejectionReason, steps);
                }
                status = UpdateStatus.APPROVED;
            }

            beforeSnapshotId = executeStep("CREATE_SNAPSHOT", () -> activities.createSnapshot(event));

            List<String> parsedElements = executeStep("PARSE", () -> activities.parseInSandbox(event));

            executeStep("VALIDATE_OUTPUT", () -> {
                activities.validateOutput(event, parsedElements);
                return null;
            });

            executeStep("WRITE_TEMP_INDEX", () -> {
                activities.writeTempIndex(event, parsedElements);
                return null;
            });

            executeStep("CANARY_VERIFY", () -> {
                activities.canaryVerify(event);
                return null;
            });

            if (paused) {
                status = UpdateStatus.PAUSED;
                Workflow.await(() -> !paused);
                status = UpdateStatus.RUNNING;
            }

            if (rollbackRequested) {
                executeStep("ROLLBACK", () -> {
                    activities.rollbackTo(beforeSnapshotId);
                    return null;
                });
                status = UpdateStatus.ROLLED_BACK;
                return UpdateResult.rolledBack("Manual rollback requested", steps);
            }

            executeStep("PROMOTE", () -> {
                activities.promoteToProduction(event, parsedElements);
                return null;
            });

            String afterSnapshotId = executeStep("CREATE_POST_SNAPSHOT", () -> activities.createSnapshot(event));

            status = UpdateStatus.COMPLETED;
            return UpdateResult.success(parsedElements.size(), steps, beforeSnapshotId, afterSnapshotId);

        } catch (Exception e) {
            status = UpdateStatus.FAILED;
            if (!steps.isEmpty()) {
                WorkflowStep lastStep = steps.get(steps.size() - 1);
                lastStep.setStatus(WorkflowStepStatus.FAILED);
                lastStep.setError(e.getMessage());
                lastStep.setCompletedAt(Instant.now());
            }
            return UpdateResult.failed(e.getMessage(), steps);
        }
    }

    @Override
    public void approve(String approver, String comment) {
        approved = true;
        activities.recordApproval(approver, comment);
    }

    @Override
    public void reject(String reason) {
        rejectionReason = reason;
    }

    @Override
    public void pause(String reason) {
        paused = true;
        pauseReason = reason;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public void rollback() {
        rollbackRequested = true;
    }

    @Override
    public UpdateStatus getStatus() {
        return status;
    }

    @Override
    public List<WorkflowStep> getStepHistory() {
        return steps;
    }

    private <T> T executeStep(String name, Supplier<T> action) {
        WorkflowStep step = new WorkflowStep(name, WorkflowStepStatus.RUNNING, Instant.now());
        steps.add(step);

        try {
            T result = action.get();
            step.setStatus(WorkflowStepStatus.COMPLETED);
            step.setCompletedAt(Instant.now());
            return result;
        } catch (Exception e) {
            step.setStatus(WorkflowStepStatus.FAILED);
            step.setError(e.getMessage());
            step.setCompletedAt(Instant.now());
            throw e;
        }
    }
}
