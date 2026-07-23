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

@Component
public class RepositorySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(RepositorySyncScheduler.class);

    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    @Value("${repository.scheduled-sync.enabled:false}")
    private boolean enabled;

    private final RepositoryConfigRepository configRepository;
    private final GitSyncService gitSyncService;

    public RepositorySyncScheduler(RepositoryConfigRepository configRepository,
                                   GitSyncService gitSyncService) {
        this.configRepository = configRepository;
        this.gitSyncService = gitSyncService;
    }

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

    private void syncSafely(com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity config) {
        try {
            gitSyncService.sync(config, TriggerType.SCHEDULED, "system");
        } catch (Exception e) {
            log.error("Scheduled sync failed for repository {}", config.getName(), e);
        }
    }
}
