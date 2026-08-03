package com.company.intelligentdiagnosis.agent.domain.workflow.activity;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.workflow.GitPushEvent;
import com.company.intelligentdiagnosis.agent.domain.workflow.RiskLevel;
import com.company.intelligentdiagnosis.agent.domain.workflow.SecurityScanResult;
import io.temporal.activity.ActivityInterface;

import java.util.List;

@ActivityInterface
public interface IndexUpdateActivities {

    void detectChanges(GitPushEvent event);

    SecurityScanResult scanSecurity(GitPushEvent event);

    RiskLevel classifyRisk(GitPushEvent event);

    String createSnapshot(GitPushEvent event, String workflowId);

    List<CodeElement> parseInSandbox(GitPushEvent event);

    void validateOutput(GitPushEvent event, List<CodeElement> elements);

    void writeTempIndex(GitPushEvent event, List<CodeElement> elements, String snapshotId);

    void canaryVerify(GitPushEvent event, String snapshotId);

    void rollbackTo(String snapshotId);

    void promoteToProduction(GitPushEvent event, List<CodeElement> elements, String snapshotId);

    void recordApproval(String approver, String comment);
}
