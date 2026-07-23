package com.company.intelligentdiagnosis.agent.domain.diagnosis;

public record CodeSnippet(
    String filePath,
    int startLine,
    int endLine,
    String content
) {
}
