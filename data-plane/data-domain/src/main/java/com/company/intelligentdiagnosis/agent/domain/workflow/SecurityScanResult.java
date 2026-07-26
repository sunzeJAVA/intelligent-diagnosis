package com.company.intelligentdiagnosis.agent.domain.workflow;

public class SecurityScanResult {
    private final boolean passed;
    private final String reason;
    private final int issuesFound;

    public SecurityScanResult(boolean passed, String reason, int issuesFound) {
        this.passed = passed;
        this.reason = reason;
        this.issuesFound = issuesFound;
    }

    public static SecurityScanResult passed() {
        return new SecurityScanResult(true, null, 0);
    }

    public static SecurityScanResult failed(String reason, int issuesFound) {
        return new SecurityScanResult(false, reason, issuesFound);
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
}
