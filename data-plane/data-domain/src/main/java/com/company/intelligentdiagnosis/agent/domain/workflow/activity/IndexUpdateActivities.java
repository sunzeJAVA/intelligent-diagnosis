package com.company.intelligentdiagnosis.agent.domain.workflow.activity;

import com.company.intelligentdiagnosis.agent.domain.workflow.GitPushEvent;
import com.company.intelligentdiagnosis.agent.domain.workflow.RiskLevel;
import com.company.intelligentdiagnosis.agent.domain.workflow.SecurityScanResult;

import java.util.List;

public interface IndexUpdateActivities {

    void detectChanges(GitPushEvent event);

    SecurityScanResult scanSecurity(GitPushEvent event);

    RiskLevel classifyRisk(GitPushEvent event);

    String createSnapshot(GitPushEvent event);

    List<String> parseInSandbox(GitPushEvent event);

    void validateOutput(GitPushEvent event, List<String> elementIds);

    void writeTempIndex(GitPushEvent event, List<String> elementIds);

    void canaryVerify(GitPushEvent event);

    void rollbackTo(String snapshotId);

    void promoteToProduction(GitPushEvent event, List<String> elementIds);

    void recordApproval(String approver, String comment);
}
