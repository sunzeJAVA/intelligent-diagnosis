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

@Component
public class RepositoryInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RepositoryInitializer.class);

    @Value("${repository.initial-sync.enabled:false}")
    private boolean enabled;

    private final RepositoryConfigRepository configRepository;
    private final GitSyncService gitSyncService;

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

    private void syncSafely(com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity config) {
        try {
            gitSyncService.sync(config, TriggerType.INITIAL, "system");
        } catch (Exception e) {
            log.error("Initial sync failed for repository {}", config.getName(), e);
        }
    }
}
