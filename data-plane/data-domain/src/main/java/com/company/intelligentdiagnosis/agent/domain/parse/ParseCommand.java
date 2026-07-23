package com.company.intelligentdiagnosis.agent.domain.parse;

import java.util.List;

public record ParseCommand(
    String repository,
    String commitHash,
    String repoPath,
    List<String> changedFiles,
    String language
) {
}
