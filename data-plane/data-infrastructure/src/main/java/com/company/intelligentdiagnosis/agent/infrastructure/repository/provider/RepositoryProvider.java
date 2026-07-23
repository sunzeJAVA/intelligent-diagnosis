package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryType;

import java.util.List;

public interface RepositoryProvider {

    boolean supports(RepositoryType type);

    RepositorySyncResult sync(RepositoryConfigEntity config) throws RepositorySyncException;

    List<String> detectChangedFiles(RepositoryConfigEntity config, String baseCommit, String headCommit);

    String getLatestCommit(RepositoryConfigEntity config);
}
