package com.company.intelligentdiagnosis.agent.infrastructure.repository.sync;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigRepository;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.TriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 仓库同步调度器
 * 定时执行仓库同步任务，支持并发处理多个仓库
 */
@Component
public class RepositorySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(RepositorySyncScheduler.class);

    /**
     * 同步运行标志，防止并发执行
     */
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    /**
     * 是否启用定时同步
     */
    @Value("${repository.scheduled-sync.enabled:false}")
    private boolean enabled;

    /**
     * 仓库配置仓库
     */
    private final RepositoryConfigRepository configRepository;

    /**
     * Git 同步服务
     */
    private final GitSyncService gitSyncService;

    /**
     * 创建实例
     *
     * @param configRepository 仓库配置仓库
     * @param gitSyncService   Git 同步服务
     */
    public RepositorySyncScheduler(RepositoryConfigRepository configRepository,
                                   GitSyncService gitSyncService) {
        this.configRepository = configRepository;
        this.gitSyncService = gitSyncService;
    }

    /**
     * 定时同步任务
     * 默认每小时执行一次，可通过配置 cron 表达式修改
     */
    @Scheduled(cron = "${repository.scheduled-sync.cron:0 0 * * * *}")
    public void scheduledSync() {
        if (!enabled) {
            log.debug("Scheduled repository sync is disabled");
            return;
        }
        if (!syncRunning.compareAndSet(false, true)) {
            log.warn("Previous scheduled sync still running, skipping this round");
            return;
        }

        try {
            var configs = configRepository.findByEnabledTrue();
            if (configs.isEmpty()) {
                log.debug("No enabled repository configurations found, skipping scheduled sync");
                return;
            }

            log.info("Starting scheduled sync for {} repositories", configs.size());

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (var config : configs) {
                    executor.submit(() -> syncSafely(config));
                }
            }

            log.info("Scheduled sync completed");
        } finally {
            syncRunning.set(false);
        }
    }

    /**
     * 安全同步单个仓库
     * 捕获异常并记录日志，不影响其他仓库的同步
     *
     * @param config 仓库配置
     */
    private void syncSafely(com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity config) {
        try {
            gitSyncService.sync(config, TriggerType.SCHEDULED, "system");
        } catch (Exception e) {
            log.error("Scheduled sync failed for repository {}", config.getName(), e);
        }
    }
}
