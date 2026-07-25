package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 仓库提供者注册中心
 * 根据仓库类型查找对应的仓库提供者实现
 */
@Component
public class RepositoryProviderRegistry {

    /**
     * 仓库提供者列表（Spring 自动注入所有实现）
     */
    private final List<RepositoryProvider> providers;

    /**
     * 创建实例
     *
     * @param providers 仓库提供者列表
     */
    public RepositoryProviderRegistry(List<RepositoryProvider> providers) {
        this.providers = providers;
    }

    /**
     * 根据仓库类型获取对应的仓库提供者
     *
     * @param type 仓库类型
     * @return 仓库提供者
     * @throws RepositorySyncException 如果未找到匹配的提供者
     */
    public RepositoryProvider getProvider(RepositoryType type) {
        return providers.stream()
            .filter(provider -> provider.supports(type))
            .findFirst()
            .orElseThrow(() -> new RepositorySyncException(
                "No repository provider found for type: " + type
            ));
    }
}
