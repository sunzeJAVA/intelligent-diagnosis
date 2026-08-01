package com.company.intelligentdiagnosis.agent.domain.workflow;

import java.util.Collections;
import java.util.List;

public class SecurityScanResult {
    private final boolean passed;
    private final String reason;
    private final int issuesFound;
    private final List<SecurityIssue> issues;

    public SecurityScanResult(boolean passed, String reason, int issuesFound, List<SecurityIssue> issues) {
        this.passed = passed;
        this.reason = reason;
        this.issuesFound = issuesFound;
        this.issues = issues != null ? List.copyOf(issues) : Collections.emptyList();
    }

    public static SecurityScanResult passed() {
        return new SecurityScanResult(true, null, 0, Collections.emptyList());
    }

    public static SecurityScanResult failed(String reason, List<SecurityIssue> issues) {
        return new SecurityScanResult(false, reason, issues != null ? issues.size() : 0, issues);
    }

    public boolean isPassed() {
        return passed;
    }

    public String getReason() {
        return reason;
    }

    public int getIssuesFound() {
        return issuesFound;
    }

    public List<SecurityIssue> getIssues() {
        return issues;
    }
}
