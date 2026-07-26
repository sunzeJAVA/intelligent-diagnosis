package com.company.intelligentdiagnosis.agent.domain.workflow;

import java.util.List;

public class UpdateResult {
    private final boolean success;
    private final int elementCount;
    private final String message;
    private final List<WorkflowStep> steps;
    private final String beforeSnapshotId;
    private final String afterSnapshotId;

    private UpdateResult(boolean success, int elementCount, String message, 
                         List<WorkflowStep> steps, String beforeSnapshotId, String afterSnapshotId) {
        this.success = success;
        this.elementCount = elementCount;
        this.message = message;
        this.steps = steps;
        this.beforeSnapshotId = beforeSnapshotId;
        this.afterSnapshotId = afterSnapshotId;
    }

    public static UpdateResult success(int elementCount, List<WorkflowStep> steps, 
                                       String beforeSnapshotId, String afterSnapshotId) {
        return new UpdateResult(true, elementCount, null, steps, beforeSnapshotId, afterSnapshotId);
    }

    public static UpdateResult rejected(String reason, List<WorkflowStep> steps) {
        return new UpdateResult(false, 0, reason, steps, null, null);
    }

    public static UpdateResult rolledBack(String reason, List<WorkflowStep> steps) {
        return new UpdateResult(false, 0, reason, steps, null, null);
    }

    public static UpdateResult failed(String reason, List<WorkflowStep> steps) {
        return new UpdateResult(false, 0, reason, steps, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getElementCount() {
        return elementCount;
    }

    public String getMessage() {
        return message;
    }

    public List<WorkflowStep> getSteps() {
        return steps;
    }

    public String getBeforeSnapshotId() {
        return beforeSnapshotId;
    }

    public String getAfterSnapshotId() {
        return afterSnapshotId;
    }
}
