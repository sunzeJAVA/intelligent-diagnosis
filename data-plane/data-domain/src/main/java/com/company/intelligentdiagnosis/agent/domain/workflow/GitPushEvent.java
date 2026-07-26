package com.company.intelligentdiagnosis.agent.domain.workflow;

import java.util.List;

public record GitPushEvent(
    String repositoryId,
    String repositoryName,
    String branch,
    String commitHash,
    String commitMessage,
    String author,
    String previousCommit,
    List<String> changedFiles,
    String repoPath,
    String language,
    String triggeredBy
) {
}
