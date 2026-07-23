package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RepositoryProviderRegistry {

    private final List<RepositoryProvider> providers;

    public RepositoryProviderRegistry(List<RepositoryProvider> providers) {
        this.providers = providers;
    }

    public RepositoryProvider getProvider(RepositoryType type) {
        return providers.stream()
            .filter(provider -> provider.supports(type))
            .findFirst()
            .orElseThrow(() -> new RepositorySyncException(
                "No repository provider found for type: " + type
            ));
    }
}
