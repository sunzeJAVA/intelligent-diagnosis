package com.company.intelligentdiagnosis.agent.infrastructure.repository.sync;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.provider.RepositoryProviderRegistry;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.provider.RepositorySyncResult;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.RepositorySyncStateEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.RepositorySyncStateRepository;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.SyncStatus;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.TriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

/**
 * Git 同步服务
 * 协调仓库同步流程，管理同步状态和事务
 */
@Service
public class GitSyncService {

    private static final Logger log = LoggerFactory.getLogger(GitSyncService.class);

    /**
     * 仓库提供者注册中心
     */
    private final RepositoryProviderRegistry providerRegistry;

    /**
     * 同步状态仓库
     */
    private final RepositorySyncStateRepository syncStateRepository;

    /**
     * 事务模板
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建实例
     *
     * @param providerRegistry      仓库提供者注册中心
     * @param syncStateRepository   同步状态仓库
     * @param transactionManager    事务管理器
     */
    public GitSyncService(RepositoryProviderRegistry providerRegistry,
                          RepositorySyncStateRepository syncStateRepository,
                          PlatformTransactionManager transactionManager) {
        this.providerRegistry = providerRegistry;
        this.syncStateRepository = syncStateRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 执行仓库同步
     *
     * @param config      仓库配置
     * @param triggerType 触发类型
     * @param triggeredBy 触发者
     * @return 同步状态实体
     */
    public RepositorySyncStateEntity sync(RepositoryConfigEntity config, TriggerType triggerType, String triggeredBy) {
        String stateId = transactionTemplate.execute(status -> createState(config, triggerType, triggeredBy));

        try {
            log.info("Starting sync for repository {} (trigger: {})", config.getName(), triggerType);
            var provider = providerRegistry.getProvider(config.getType());
            RepositorySyncResult result = provider.sync(config);

            log.info("Successfully synced repository {} to commit {} ({} changed files)",
                config.getName(), result.latestCommit(), result.changedFiles().size());

            return transactionTemplate.execute(status -> markSuccess(stateId, result));
        } catch (Exception e) {
            log.error("Failed to sync repository {}", config.getName(), e);
            return transactionTemplate.execute(status -> markFailed(stateId, e));
        }
    }

    /**
     * 创建同步状态记录
     *
     * @param config      仓库配置
     * @param triggerType 触发类型
     * @param triggeredBy 触发者
     * @return 状态 ID
     */
    private String createState(RepositoryConfigEntity config, TriggerType triggerType, String triggeredBy) {
        String stateId = UUID.randomUUID().toString();
        RepositorySyncStateEntity state = new RepositorySyncStateEntity();
        state.setId(stateId);
        state.setRepository(config);
        state.setStatus(SyncStatus.SYNCING);
        state.setTriggerType(triggerType);
        state.setTriggeredBy(triggeredBy);
        state.setStartedAt(Instant.now());
        state.setCreatedAt(Instant.now());
        syncStateRepository.save(state);
        return stateId;
    }

    /**
     * 标记同步成功
     *
     * @param stateId 状态 ID
     * @param result  同步结果
     * @return 更新后的同步状态实体
     */
    private RepositorySyncStateEntity markSuccess(String stateId, RepositorySyncResult result) {
        RepositorySyncStateEntity state = syncStateRepository.findById(stateId)
            .orElseThrow(() -> new IllegalStateException("Sync state not found: " + stateId));
        state.setStatus(SyncStatus.SUCCESS);
        state.setLatestCommit(result.latestCommit());
        state.setPreviousCommit(result.previousCommit());
        state.setChangedFiles(result.changedFiles().size());
        state.setCompletedAt(Instant.now());
        return syncStateRepository.save(state);
    }

    /**
     * 标记同步失败
     *
     * @param stateId 状态 ID
     * @param e       异常
     * @return 更新后的同步状态实体
     */
    private RepositorySyncStateEntity markFailed(String stateId, Exception e) {
        RepositorySyncStateEntity state = syncStateRepository.findById(stateId)
            .orElseThrow(() -> new IllegalStateException("Sync state not found: " + stateId));
        state.setStatus(SyncStatus.FAILED);
        state.setErrorMessage(e.getMessage());
        state.setCompletedAt(Instant.now());
        return syncStateRepository.save(state);
    }
}
