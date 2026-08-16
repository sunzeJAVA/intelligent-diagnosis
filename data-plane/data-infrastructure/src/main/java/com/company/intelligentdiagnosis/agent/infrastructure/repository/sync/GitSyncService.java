package com.company.intelligentdiagnosis.agent.infrastructure.repository.sync;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.provider.RepositoryProviderRegistry;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.provider.RepositorySyncResult;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.RepositorySyncStateEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.RepositorySyncStateRepository;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.SyncStatus;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.TriggerType;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
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

    private static final String WORKFLOW_TYPE = "IndexUpdateWorkflow";
    private static final String TASK_QUEUE = "index-update-task-queue";

    /**
     * 仓库提供者注册中心
     */
    private final RepositoryProviderRegistry providerRegistry;

    /**
     * 同步状态仓库
     */
    private final RepositorySyncStateRepository syncStateRepository;

    /**
     * 独立事务模板（REQUIRES_NEW），确保状态更新立即提交，不受外部事务影响
     */
    private final TransactionTemplate requiresNewTransactionTemplate;

    /**
     * Temporal 工作流客户端（可选依赖，不可用时 sync 仍可正常执行，只是不触发索引工作流）
     */
    private final WorkflowClient workflowClient;

    /**
     * 创建实例
     *
     * @param providerRegistry      仓库提供者注册中心
     * @param syncStateRepository   同步状态仓库
     * @param transactionManager    事务管理器
     * @param workflowClient        Temporal 工作流客户端（可选，允许为 null）
     */
    @Autowired
    public GitSyncService(RepositoryProviderRegistry providerRegistry,
                          RepositorySyncStateRepository syncStateRepository,
                          PlatformTransactionManager transactionManager,
                          WorkflowClient workflowClient) {
        this.providerRegistry = providerRegistry;
        this.syncStateRepository = syncStateRepository;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.workflowClient = workflowClient;
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
        // 1. 在独立事务中创建 SYNCING 状态记录（立即提交）
        String stateId = requiresNewTransactionTemplate.execute(status -> createState(config, triggerType, triggeredBy));

        // 2. git 操作在事务外执行（可能耗时很长）
        try {
            log.info("Starting sync for repository {} (trigger: {})", config.getName(), triggerType);
            var provider = providerRegistry.getProvider(config.getType());
            RepositorySyncResult result = provider.sync(config);

            log.info("Successfully synced repository {} to commit {} ({} changed files)",
                config.getName(), result.latestCommit(), result.changedFiles().size());

            // 3. 在独立事务中标记成功（立即提交）
            RepositorySyncStateEntity state = requiresNewTransactionTemplate.execute(s -> markSuccess(stateId, result));

            // 4. sync 成功后触发索引更新工作流（有变更或首次克隆时）
            triggerIndexUpdateWorkflow(config, result, triggeredBy);

            return state;
        } catch (Exception e) {
            log.error("Failed to sync repository {}", config.getName(), e);
            // 5. 在独立事务中标记失败（立即提交）
            return requiresNewTransactionTemplate.execute(status -> markFailed(stateId, e));
        }
    }

    /**
     * 触发索引更新工作流
     * sync 成功后总是触发（parse worker 在 changedFiles 为空时会全量解析整个仓库）；
     * Temporal 不可用时静默降级（只记录日志）
     *
     * @param config      仓库配置
     * @param result      同步结果
     * @param triggeredBy 触发者
     */
    private void triggerIndexUpdateWorkflow(RepositoryConfigEntity config, RepositorySyncResult result, String triggeredBy) {
        if (workflowClient == null) {
            log.warn("Temporal WorkflowClient not available, skipping index update workflow for repository {}",
                config.getName());
            return;
        }

        try {
            String workflowId = "index-update-" + config.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);

            WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId(workflowId)
                .build();

            // 使用 untyped stub（typed stub 最多支持 6 个参数，update 有 11 个参数）
            WorkflowStub stub = workflowClient.newUntypedWorkflowStub(WORKFLOW_TYPE, options);

            // 异步启动工作流（不等待完成）
            // changedFiles 为空时，parse worker 会全量解析所有 .java 文件
            stub.start("update",
                config.getId(),
                config.getName(),
                config.getBranch(),
                result.latestCommit(),
                "",  // commitMessage 暂留空，可后续从 git 获取
                "",  // author 暂留空
                result.previousCommit() != null ? result.previousCommit() : "",  // 避免 null 导致序列化错位
                result.changedFiles(),
                config.getLocalPath(),
                "java",  // 默认语言
                triggeredBy
            );

            log.info("Started index update workflow {} for repository {} (commit: {}, changedFiles: {}, freshClone: {})",
                workflowId, config.getName(), result.latestCommit(), result.changedFiles().size(), result.freshClone());
        } catch (Exception e) {
            // 工作流启动失败不影响 sync 结果，只记录日志
            log.warn("Failed to start index update workflow for repository {}: {}",
                config.getName(), e.getMessage());
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
        state.setErrorMessage(buildErrorMessage(e));
        state.setCompletedAt(Instant.now());
        return syncStateRepository.save(state);
    }

    /**
     * 构建完整的错误信息（包含异常链）
     * 避免只记录外层包装异常而丢失原始 cause
     *
     * @param e 异常
     * @return 完整的错误信息
     */
    private String buildErrorMessage(Exception e) {
        StringBuilder sb = new StringBuilder(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        Throwable cause = e.getCause();
        while (cause != null) {
            sb.append(" | Caused by: ").append(cause.getClass().getSimpleName())
              .append(": ").append(cause.getMessage());
            cause = cause.getCause();
        }
        // 限制长度，避免 TEXT 字段过大
        String result = sb.toString();
        return result.length() > 2000 ? result.substring(0, 2000) + "..." : result;
    }
}
