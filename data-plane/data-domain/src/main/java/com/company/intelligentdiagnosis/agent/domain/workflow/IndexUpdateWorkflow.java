package com.company.intelligentdiagnosis.agent.domain.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowMethod;

import java.util.List;

public interface IndexUpdateWorkflow {

    @WorkflowMethod(taskQueue = "index-update-task-queue")
    UpdateResult update(GitPushEvent event);

    @SignalMethod
    void approve(String approver, String comment);

    @SignalMethod
    void reject(String reason);

    @SignalMethod
    void pause(String reason);

    @SignalMethod
    void resume();

    @SignalMethod
    void rollback();

    @QueryMethod
    UpdateStatus getStatus();

    @QueryMethod
    List<WorkflowStep> getStepHistory();
}
