package com.company.intelligentdiagnosis.control.domain.workflow;

import java.util.List;

public interface WorkflowService {

    List<WorkflowInfo> listWorkflows();

    WorkflowInfo getWorkflow(String workflowId);

    void pauseWorkflow(String workflowId);

    void resumeWorkflow(String workflowId);

    void rollbackWorkflow(String workflowId);

    void approveWorkflow(String workflowId, String approver, String comment);

    void rejectWorkflow(String workflowId, String reason);

    String startIndexUpdateWorkflow(String repositoryId, String repositoryName,
                                    String branch, String commitHash, String commitMessage,
                                    String author, String previousCommit, List<String> changedFiles,
                                    String repoPath, String language, String triggeredBy);
}
