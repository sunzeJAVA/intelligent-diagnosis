package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.AuthType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Component;

/**
 * Git 凭证工厂
 * 根据仓库配置创建对应的 JGit 凭证提供者
 */
@Component
public class GitCredentialFactory {

    /**
     * 根据仓库配置创建凭证提供者
     *
     * @param config 仓库配置实体
     * @return JGit 凭证提供者，如果不需要认证则返回 null
     */
    public CredentialsProvider create(RepositoryConfigEntity config) {
        AuthType authType = config.getAuthType();
        if (authType == null || authType == AuthType.NONE) {
            return null;
        }

        return switch (authType) {
            case TOKEN -> new UsernamePasswordCredentialsProvider(config.getAuthToken(), "");
            case USERNAME_PASSWORD -> new UsernamePasswordCredentialsProvider(
                config.getAuthUsername(),
                config.getAuthPassword()
            );
            case SSH_KEY -> null;
            default -> null;
        };
    }
}
