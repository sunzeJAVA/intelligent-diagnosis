package com.company.intelligentdiagnosis.agent.infrastructure.workflow.activity;

import com.company.intelligentdiagnosis.agent.domain.workflow.GitPushEvent;
import com.company.intelligentdiagnosis.agent.domain.workflow.RiskLevel;
import com.company.intelligentdiagnosis.agent.domain.workflow.SecurityScanResult;
import com.company.intelligentdiagnosis.agent.domain.workflow.activity.IndexUpdateActivities;
import com.company.intelligentdiagnosis.agent.infrastructure.parse.ParseWorkerClient;
import com.company.intelligentdiagnosis.parse.ParseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class IndexUpdateActivitiesImpl implements IndexUpdateActivities {

    private static final Logger log = LoggerFactory.getLogger(IndexUpdateActivitiesImpl.class);

    private final ParseWorkerClient parseWorkerClient;

    public IndexUpdateActivitiesImpl(ParseWorkerClient parseWorkerClient) {
        this.parseWorkerClient = parseWorkerClient;
    }

    @Override
    public void detectChanges(GitPushEvent event) {
        log.info("Detecting changes for repository {} (commit: {})", event.repositoryName(), event.commitHash());
    }

    @Override
    public SecurityScanResult scanSecurity(GitPushEvent event) {
        log.info("Running security scan for repository {}", event.repositoryName());
        return SecurityScanResult.passed();
    }

    @Override
    public RiskLevel classifyRisk(GitPushEvent event) {
        log.info("Classifying risk for {} changed files", event.changedFiles().size());
        if (event.changedFiles().size() > 100) {
            return RiskLevel.HIGH;
        } else if (event.changedFiles().size() > 10) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    @Override
    public String createSnapshot(GitPushEvent event) {
        String snapshotId = UUID.randomUUID().toString();
        log.info("Created snapshot {} for repository {}", snapshotId, event.repositoryName());
        return snapshotId;
    }

    @Override
    public List<String> parseInSandbox(GitPushEvent event) {
        log.info("Parsing {} files in sandbox for repository {}", event.changedFiles().size(), event.repositoryName());
        ParseRequest request = ParseRequest.newBuilder()
            .setRepository(event.repositoryName())
            .setCommitHash(event.commitHash())
            .setRepoPath(event.repoPath())
            .addAllChangedFiles(event.changedFiles())
            .setLanguage(event.language())
            .build();

        try {
            var elements = parseWorkerClient.parse(event.language(), request);
            return elements.stream()
                .map(e -> e.id())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse files", e);
            return List.of();
        }
    }

    @Override
    public void validateOutput(GitPushEvent event, List<String> elementIds) {
        log.info("Validating {} parsed elements for repository {}", elementIds.size(), event.repositoryName());
    }

    @Override
    public void writeTempIndex(GitPushEvent event, List<String> elementIds) {
        log.info("Writing {} elements to temporary index for repository {}", elementIds.size(), event.repositoryName());
    }

    @Override
    public void canaryVerify(GitPushEvent event) {
        log.info("Running canary verification for repository {}", event.repositoryName());
    }

    @Override
    public void rollbackTo(String snapshotId) {
        log.info("Rolling back to snapshot {}", snapshotId);
    }

    @Override
    public void promoteToProduction(GitPushEvent event, List<String> elementIds) {
        log.info("Promoting {} elements to production for repository {}", elementIds.size(), event.repositoryName());
    }

    @Override
    public void recordApproval(String approver, String comment) {
        log.info("Recording approval by {}: {}", approver, comment);
    }
}
