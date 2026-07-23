package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.AuthType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Component;

@Component
public class GitCredentialFactory {

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
            case SSH_KEY -> null; // SSH authentication is handled via TransportConfigCallback
            default -> null;
        };
    }
}
