package com.company.intelligentdiagnosis.agent.domain.workflow;

import java.time.Instant;

public class WorkflowStep {
    private String name;
    private WorkflowStepStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String error;

    public WorkflowStep() {
    }

    public WorkflowStep(String name, WorkflowStepStatus status, Instant startedAt) {
        this.name = name;
        this.status = status;
        this.startedAt = startedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorkflowStepStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStepStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
