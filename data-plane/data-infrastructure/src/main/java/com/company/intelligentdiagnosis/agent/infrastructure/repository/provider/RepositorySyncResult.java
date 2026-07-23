package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

import java.util.List;

public record RepositorySyncResult(
    String latestCommit,
    String previousCommit,
    List<String> changedFiles,
    boolean freshClone
) {
}
