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

/**
 * 仓库应用服务
 * 提供仓库配置管理和同步操作的业务逻辑
 */
@Service
public class RepositoryApplicationService {

    private final RepositoryConfigRepository configRepository;
    private final RepositorySyncStateRepository syncStateRepository;
    private final GitSyncService gitSyncService;
    private final RepositoryProviderRegistry providerRegistry;

    /**
     * 构造函数
     *
     * @param configRepository     仓库配置仓储
     * @param syncStateRepository  同步状态仓储
     * @param gitSyncService       Git 同步服务
     * @param providerRegistry     仓库提供者注册中心
     */
    public RepositoryApplicationService(RepositoryConfigRepository configRepository,
                                        RepositorySyncStateRepository syncStateRepository,
                                        GitSyncService gitSyncService,
                                        RepositoryProviderRegistry providerRegistry) {
        this.configRepository = configRepository;
        this.syncStateRepository = syncStateRepository;
        this.gitSyncService = gitSyncService;
        this.providerRegistry = providerRegistry;
    }

    /**
     * 查询所有仓库配置
     *
     * @return 仓库配置列表
     */
    @Transactional(readOnly = true)
    public List<RepositoryConfigEntity> listRepositories() {
        return configRepository.findAll();
    }

    /**
     * 根据 ID 查询仓库配置
     *
     * @param id 仓库 ID
     * @return 仓库配置
     * @throws RepositorySyncException 仓库不存在时抛出
     */
    @Transactional(readOnly = true)
    public RepositoryConfigEntity getRepository(String id) {
        return configRepository.findById(id)
            .orElseThrow(() -> new RepositorySyncException("Repository not found: " + id));
    }

    /**
     * 创建仓库配置
     *
     * @param command 创建命令
     * @return 创建后的仓库配置
     * @throws RepositorySyncException 仓库已存在时抛出
     */
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

    /**
     * 更新仓库配置
     *
     * @param id      仓库 ID
     * @param command 更新命令
     * @return 更新后的仓库配置
     * @throws RepositorySyncException 仓库不存在时抛出
     */
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

    /**
     * 删除仓库配置
     *
     * @param id 仓库 ID
     */
    @Transactional
    public void deleteRepository(String id) {
        configRepository.deleteById(id);
    }

    /**
     * 同步仓库
     * 注意：不使用 @Transactional，因为 git 操作耗时长，不应在数据库事务中执行。
     * 事务由 GitSyncService 内部通过 REQUIRES_NEW 独立管理。
     *
     * @param id          仓库 ID
     * @param triggeredBy 触发者
     * @return 同步状态记录
     * @throws RepositorySyncException 仓库不存在时抛出
     */
    public RepositorySyncStateEntity syncRepository(String id, String triggeredBy) {
        RepositoryConfigEntity config = configRepository.findById(id)
            .orElseThrow(() -> new RepositorySyncException("Repository not found: " + id));
        return gitSyncService.sync(config, TriggerType.MANUAL, triggeredBy);
    }

    /**
     * 获取仓库同步历史
     *
     * @param repositoryId 仓库 ID
     * @return 同步状态记录列表（按时间倒序）
     */
    @Transactional(readOnly = true)
    public List<RepositorySyncStateEntity> getSyncHistory(String repositoryId) {
        return syncStateRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId);
    }

    /**
     * 验证仓库提供者是否存在
     *
     * @param type 仓库类型
     * @throws RepositorySyncException 提供者不存在时抛出
     */
    private void validateProvider(RepositoryType type) {
        providerRegistry.getProvider(type);
    }

    /**
     * 创建仓库命令
     */
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

    /**
     * 更新仓库命令
     */
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
