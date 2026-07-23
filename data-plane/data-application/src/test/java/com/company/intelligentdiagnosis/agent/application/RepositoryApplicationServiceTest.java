package com.company.intelligentdiagnosis.agent.application;

import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.AuthType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigRepository;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.provider.RepositoryProvider;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.provider.RepositoryProviderRegistry;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.provider.RepositorySyncException;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.RepositorySyncStateEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.RepositorySyncStateRepository;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.TriggerType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.sync.GitSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryApplicationServiceTest {

    @Mock
    private RepositoryConfigRepository configRepository;

    @Mock
    private RepositorySyncStateRepository syncStateRepository;

    @Mock
    private GitSyncService gitSyncService;

    @Mock
    private RepositoryProviderRegistry providerRegistry;

    @InjectMocks
    private RepositoryApplicationService service;

    @Test
    void shouldCreateRepository() {
        when(configRepository.existsByName("my-repo")).thenReturn(false);
        when(configRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(providerRegistry.getProvider(RepositoryType.GITHUB)).thenReturn(mock(RepositoryProvider.class));

        RepositoryConfigEntity result = service.createRepository(
            new RepositoryApplicationService.CreateRepositoryCommand(
                "my-repo", "My Repo", RepositoryType.GITHUB, "http://example.com/repo.git",
                "main", "/tmp/repo", true, AuthType.NONE, null, null, null, null
            )
        );

        assertThat(result.getName()).isEqualTo("my-repo");
        assertThat(result.getType()).isEqualTo(RepositoryType.GITHUB);
        assertThat(result.getDisplayName()).isEqualTo("My Repo");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateRepositoryName() {
        when(configRepository.existsByName("my-repo")).thenReturn(true);

        assertThatThrownBy(() -> service.createRepository(
            new RepositoryApplicationService.CreateRepositoryCommand(
                "my-repo", "My Repo", RepositoryType.GITHUB, "http://example.com/repo.git",
                "main", "/tmp/repo", true, AuthType.NONE, null, null, null, null
            )
        )).isInstanceOf(RepositorySyncException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void shouldSyncRepository() {
        RepositoryConfigEntity config = new RepositoryConfigEntity();
        config.setId("id-1");
        config.setName("my-repo");
        when(configRepository.findById("id-1")).thenReturn(Optional.of(config));
        RepositorySyncStateEntity state = new RepositorySyncStateEntity();
        when(gitSyncService.sync(config, TriggerType.MANUAL, "user")).thenReturn(state);

        RepositorySyncStateEntity result = service.syncRepository("id-1", "user");

        assertThat(result).isEqualTo(state);
        verify(gitSyncService).sync(eq(config), eq(TriggerType.MANUAL), eq("user"));
    }

    @Test
    void shouldThrowWhenRepositoryNotFound() {
        when(configRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRepository("missing"))
            .isInstanceOf(RepositorySyncException.class)
            .hasMessageContaining("not found");
    }
}
