package com.company.intelligentdiagnosis.agent.infrastructure.repository.sync;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigRepository;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.TriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

/**
 * 仓库初始化器
 * 在应用启动时执行仓库初始同步
 */
@Component
public class RepositoryInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RepositoryInitializer.class);

    /**
     * 是否启用初始同步
     */
    @Value("${repository.initial-sync.enabled:false}")
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
    public RepositoryInitializer(RepositoryConfigRepository configRepository,
                                 GitSyncService gitSyncService) {
        this.configRepository = configRepository;
        this.gitSyncService = gitSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Repository initial sync is disabled");
            return;
        }

        var configs = configRepository.findByEnabledTrue();
        if (configs.isEmpty()) {
            log.info("No enabled repository configurations found, skipping initial sync");
            return;
        }

        log.info("Starting initial sync for {} repositories", configs.size());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var config : configs) {
                executor.submit(() -> syncSafely(config));
            }
        }

        log.info("Initial sync submissions completed");
    }

    /**
     * 安全同步单个仓库
     * 捕获异常并记录日志，不影响其他仓库的同步
     *
     * @param config 仓库配置
     */
    private void syncSafely(com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity config) {
        try {
            gitSyncService.sync(config, TriggerType.INITIAL, "system");
        } catch (Exception e) {
            log.error("Initial sync failed for repository {}", config.getName(), e);
        }
    }
}
