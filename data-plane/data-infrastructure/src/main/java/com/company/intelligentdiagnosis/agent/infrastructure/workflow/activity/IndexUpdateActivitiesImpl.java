package com.company.intelligentdiagnosis.agent.infrastructure.workflow.activity;

import com.company.intelligentdiagnosis.agent.infrastructure.snapshot.SnapshotApplicationService;
import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.enrichment.CodeElementEnricher;
import com.company.intelligentdiagnosis.agent.domain.snapshot.IndexSnapshot;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotStatus;
import com.company.intelligentdiagnosis.agent.domain.workflow.GitPushEvent;
import com.company.intelligentdiagnosis.agent.domain.workflow.RiskLevel;
import com.company.intelligentdiagnosis.agent.domain.workflow.SecurityScanResult;
import com.company.intelligentdiagnosis.agent.domain.workflow.SecurityScanner;
import com.company.intelligentdiagnosis.agent.domain.workflow.activity.IndexUpdateActivities;
import com.company.intelligentdiagnosis.agent.infrastructure.parse.ParseWorkerClient;
import com.company.intelligentdiagnosis.agent.infrastructure.security.SecurityScanProperties;
import com.company.intelligentdiagnosis.parse.ParseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class IndexUpdateActivitiesImpl implements IndexUpdateActivities {

    private static final Logger log = LoggerFactory.getLogger(IndexUpdateActivitiesImpl.class);

    private final ParseWorkerClient parseWorkerClient;
    private final SnapshotApplicationService snapshotApplicationService;
    private final SecurityScanner securityScanner;
    private final SecurityScanProperties securityScanProperties;
    private final CodeElementEnricher enricher;

    public IndexUpdateActivitiesImpl(ParseWorkerClient parseWorkerClient,
                                     SnapshotApplicationService snapshotApplicationService,
                                     SecurityScanner securityScanner,
                                     SecurityScanProperties securityScanProperties,
                                     CodeElementEnricher enricher) {
        this.parseWorkerClient = parseWorkerClient;
        this.snapshotApplicationService = snapshotApplicationService;
        this.securityScanner = securityScanner;
        this.securityScanProperties = securityScanProperties;
        this.enricher = enricher;
    }

    @Override
    public void detectChanges(GitPushEvent event) {
        log.info("Detecting changes for repository {} (commit: {})", event.repositoryName(), event.commitHash());
    }

    @Override
    public SecurityScanResult scanSecurity(GitPushEvent event) {
        log.info("Running security scan for repository {}", event.repositoryName());
        if (!securityScanProperties.isEnabled()) {
            log.info("Security scan is disabled, skipping");
            return SecurityScanResult.passed();
        }

        SecurityScanResult result = securityScanner.scan(event);
        if (!result.isPassed()) {
            long highCount = result.getIssues().stream()
                .filter(i -> i.severity() == com.company.intelligentdiagnosis.agent.domain.workflow.SecurityIssue.Severity.HIGH)
                .count();
            log.warn("Security scan found {} issues (HIGH {}) for repository {}",
                result.getIssuesFound(), highCount, event.repositoryName());

            if (!securityScanProperties.isBlockOnHigh()) {
                log.warn("security.scan.block-on-high is disabled, continuing workflow");
                return SecurityScanResult.passed();
            }
            if (highCount == 0) {
                log.warn("Security scan found only LOW/MEDIUM issues, continuing workflow");
                return SecurityScanResult.passed();
            }
        }
        return result;
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
    public String createSnapshot(GitPushEvent event, String workflowId) {
        IndexSnapshot snapshot = snapshotApplicationService.createPreSnapshot(event, workflowId);
        log.info("Created pre-snapshot {} for repository {}", snapshot.id(), event.repositoryName());
        return snapshot.id();
    }

    @Override
    public List<CodeElement> parseInSandbox(GitPushEvent event) {
        log.info("Parsing {} files in sandbox for repository {}", event.changedFiles().size(), event.repositoryName());
        ParseRequest request = ParseRequest.newBuilder()
            .setRepository(event.repositoryName())
            .setCommitHash(event.commitHash())
            .setRepoPath(event.repoPath())
            .addAllChangedFiles(event.changedFiles())
            .setLanguage(event.language())
            .build();

        var elements = parseWorkerClient.parse(event.language(), request);
        log.info("Parsed {} elements for repository {}", elements.size(), event.repositoryName());
        return elements;
    }

    @Override
    public void validateOutput(GitPushEvent event, List<CodeElement> elements) {
        log.info("Validating {} parsed elements for repository {}", elements.size(), event.repositoryName());
        if (elements.isEmpty()) {
            throw new IllegalStateException("No elements parsed for repository: " + event.repositoryName());
        }
        long uniqueIds = elements.stream().map(CodeElement::id).distinct().count();
        if (uniqueIds != elements.size()) {
            throw new IllegalStateException("Duplicate element IDs detected");
        }
    }

    @Override
    public void writeTempIndex(GitPushEvent event, List<CodeElement> elements, String snapshotId) {
        log.info("Writing {} elements to temporary index for repository {}", elements.size(), event.repositoryName());
        List<CodeElement> enriched = enricher.enrich(elements);
        List<String> elementIds = enriched.stream().map(CodeElement::id).collect(Collectors.toList());
        snapshotApplicationService.recordSandboxIndex(event, snapshotId, enriched, elementIds);
    }

    @Override
    public void canaryVerify(GitPushEvent event, String snapshotId) {
        log.info("Running canary verification for repository {}", event.repositoryName());
        IndexSnapshot snapshot = snapshotApplicationService.validateSnapshot(snapshotId);
        if (snapshot.status() == SnapshotStatus.FAILED) {
            throw new IllegalStateException("Canary verification failed: " + snapshot.validations());
        }
    }

    @Override
    public void rollbackTo(String snapshotId) {
        log.info("Rolling back to snapshot {}", snapshotId);
        snapshotApplicationService.rollbackTo(snapshotId);
    }

    @Override
    public void promoteToProduction(GitPushEvent event, List<CodeElement> elements, String snapshotId) {
        log.info("Promoting {} elements to production for repository {}", elements.size(), event.repositoryName());
        snapshotApplicationService.promoteSnapshot(snapshotId);
    }

    @Override
    public void recordApproval(String approver, String comment) {
        log.info("Recording approval by {}: {}", approver, comment);
    }
}
