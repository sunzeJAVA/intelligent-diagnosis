package com.company.intelligentdiagnosis.agent.domain.workflow;

/**
 * 安全扫描发现的问题
 */
public record SecurityIssue(
    String ruleId,
    Severity severity,
    String filePath,
    int lineNumber,
    String message,
    String snippet
) {

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }
}
