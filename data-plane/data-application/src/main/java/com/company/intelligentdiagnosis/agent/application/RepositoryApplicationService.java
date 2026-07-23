package com.company.intelligentdiagnosis.agent.application;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.AuthType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigRepository;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.provider.RepositoryProviderRegistry;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.provider.RepositorySyncException;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.RepositorySyncStateEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.RepositorySyncStateRepository;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.TriggerType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.sync.GitSyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RepositoryApplicationService {

    private final RepositoryConfigRepository configRepository;
    private final RepositorySyncStateRepository syncStateRepository;
    private final GitSyncService gitSyncService;
    private final RepositoryProviderRegistry providerRegistry;

    public RepositoryApplicationService(RepositoryConfigRepository configRepository,
                                        RepositorySyncStateRepository syncStateRepository,
                                        GitSyncService gitSyncService,
                                        RepositoryProviderRegistry providerRegistry) {
        this.configRepository = configRepository;
        this.syncStateRepository = syncStateRepository;
        this.gitSyncService = gitSyncService;
        this.providerRegistry = providerRegistry;
    }

    @Transactional(readOnly = true)
    public List<RepositoryConfigEntity> listRepositories() {
        return configRepository.findAll();
    }

    @Transactional(readOnly = true)
    public RepositoryConfigEntity getRepository(String id) {
        return configRepository.findById(id)
            .orElseThrow(() -> new RepositorySyncException("Repository not found: " + id));
    }

    @Transactional
    public RepositoryConfigEntity createRepository(CreateRepositoryCommand command) {
        if (configRepository.existsByName(command.name())) {
            throw new RepositorySyncException("Repository already exists: " + command.name());
        }

        validateProvider(command.type());

        RepositoryConfigEntity entity = new RepositoryConfigEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(command.name());
        entity.setDisplayName(command.displayName());
        entity.setType(command.type());
        entity.setUrl(command.url());
        entity.setBranch(command.branch());
        entity.setLocalPath(command.localPath());
        entity.setEnabled(command.enabled());
        entity.setAuthType(command.authType());
        entity.setAuthToken(command.authToken());
        entity.setAuthUsername(command.authUsername());
        entity.setAuthPassword(command.authPassword());
        entity.setAuthSshKeyPath(command.authSshKeyPath());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        return configRepository.save(entity);
    }

    @Transactional
    public RepositoryConfigEntity updateRepository(String id, UpdateRepositoryCommand command) {
        RepositoryConfigEntity entity = configRepository.findById(id)
            .orElseThrow(() -> new RepositorySyncException("Repository not found: " + id));

        validateProvider(command.type());

        entity.setDisplayName(command.displayName());
        entity.setType(command.type());
        entity.setUrl(command.url());
        entity.setBranch(command.branch());
        entity.setLocalPath(command.localPath());
        entity.setEnabled(command.enabled());
        entity.setAuthType(command.authType());
        entity.setAuthToken(command.authToken());
        entity.setAuthUsername(command.authUsername());
        entity.setAuthPassword(command.authPassword());
        entity.setAuthSshKeyPath(command.authSshKeyPath());
        entity.setUpdatedAt(Instant.now());

        return configRepository.save(entity);
    }

    @Transactional
    public void deleteRepository(String id) {
        configRepository.deleteById(id);
    }

    @Transactional
    public RepositorySyncStateEntity syncRepository(String id, String triggeredBy) {
        RepositoryConfigEntity config = configRepository.findById(id)
            .orElseThrow(() -> new RepositorySyncException("Repository not found: " + id));
        return gitSyncService.sync(config, TriggerType.MANUAL, triggeredBy);
    }

    @Transactional(readOnly = true)
    public List<RepositorySyncStateEntity> getSyncHistory(String repositoryId) {
        return syncStateRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId);
    }

    private void validateProvider(RepositoryType type) {
        providerRegistry.getProvider(type);
    }

    public record CreateRepositoryCommand(
        String name,
        String displayName,
        RepositoryType type,
        String url,
        String branch,
        String localPath,
        boolean enabled,
        AuthType authType,
        String authToken,
        String authUsername,
        String authPassword,
        String authSshKeyPath
    ) {
    }

    public record UpdateRepositoryCommand(
        String displayName,
        RepositoryType type,
        String url,
        String branch,
        String localPath,
        boolean enabled,
        AuthType authType,
        String authToken,
        String authUsername,
        String authPassword,
        String authSshKeyPath
    ) {
    }
}
