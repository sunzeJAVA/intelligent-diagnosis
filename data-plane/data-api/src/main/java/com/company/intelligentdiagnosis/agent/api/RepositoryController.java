package com.company.intelligentdiagnosis.agent.api;

import com.company.intelligentdiagnosis.agent.application.RepositoryApplicationService;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.AuthType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryConfigEntity;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.config.RepositoryType;
import com.company.intelligentdiagnosis.agent.infrastructure.repository.state.RepositorySyncStateEntity;
import com.company.intelligentdiagnosis.agent.application.RepositoryApplicationService.CreateRepositoryCommand;
import com.company.intelligentdiagnosis.agent.application.RepositoryApplicationService.UpdateRepositoryCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/data/repositories")
public class RepositoryController {

    private final RepositoryApplicationService repositoryApplicationService;

    public RepositoryController(RepositoryApplicationService repositoryApplicationService) {
        this.repositoryApplicationService = repositoryApplicationService;
    }

    @GetMapping
    public ResponseEntity<List<RepositoryDto>> listRepositories() {
        List<RepositoryDto> result = repositoryApplicationService.listRepositories().stream()
            .map(this::toDto)
            .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepositoryDto> getRepository(@PathVariable String id) {
        return ResponseEntity.ok(toDto(repositoryApplicationService.getRepository(id)));
    }

    @PostMapping
    public ResponseEntity<RepositoryDto> createRepository(@RequestBody CreateRepositoryRequest request) {
        RepositoryConfigEntity entity = repositoryApplicationService.createRepository(toCommand(request));
        return ResponseEntity.created(URI.create("/api/data/repositories/" + entity.getId()))
            .body(toDto(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RepositoryDto> updateRepository(@PathVariable String id,
                                                          @RequestBody UpdateRepositoryRequest request) {
        return ResponseEntity.ok(toDto(repositoryApplicationService.updateRepository(id, toCommand(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepository(@PathVariable String id) {
        repositoryApplicationService.deleteRepository(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<SyncStateDto> syncRepository(@PathVariable String id) {
        return ResponseEntity.ok(toDto(repositoryApplicationService.syncRepository(id, "admin")));
    }

    @GetMapping("/{id}/sync-history")
    public ResponseEntity<List<SyncStateDto>> getSyncHistory(@PathVariable String id) {
        List<SyncStateDto> result = repositoryApplicationService.getSyncHistory(id).stream()
            .map(this::toDto)
            .toList();
        return ResponseEntity.ok(result);
    }

    private RepositoryDto toDto(RepositoryConfigEntity entity) {
        return new RepositoryDto(
            entity.getId(),
            entity.getName(),
            entity.getDisplayName(),
            entity.getType(),
            entity.getUrl(),
            entity.getBranch(),
            entity.getLocalPath(),
            entity.isEnabled(),
            entity.getAuthType(),
            entity.getAuthUsername(),
            entity.getAuthSshKeyPath(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private SyncStateDto toDto(RepositorySyncStateEntity entity) {
        return new SyncStateDto(
            entity.getId(),
            entity.getRepository().getId(),
            entity.getStatus(),
            entity.getStartedAt(),
            entity.getCompletedAt(),
            entity.getLatestCommit(),
            entity.getPreviousCommit(),
            entity.getChangedFiles(),
            entity.getErrorMessage(),
            entity.getTriggerType(),
            entity.getTriggeredBy(),
            entity.getCreatedAt()
        );
    }

    private CreateRepositoryCommand toCommand(CreateRepositoryRequest request) {
        return new CreateRepositoryCommand(
            request.name(),
            request.displayName(),
            request.type(),
            request.url(),
            request.branch(),
            request.localPath(),
            request.enabled(),
            request.authType(),
            request.authToken(),
            request.authUsername(),
            request.authPassword(),
            request.authSshKeyPath()
        );
    }

    private UpdateRepositoryCommand toCommand(UpdateRepositoryRequest request) {
        return new UpdateRepositoryCommand(
            request.displayName(),
            request.type(),
            request.url(),
            request.branch(),
            request.localPath(),
            request.enabled(),
            request.authType(),
            request.authToken(),
            request.authUsername(),
            request.authPassword(),
            request.authSshKeyPath()
        );
    }

    public record RepositoryDto(
        String id,
        String name,
        String displayName,
        RepositoryType type,
        String url,
        String branch,
        String localPath,
        boolean enabled,
        AuthType authType,
        String authUsername,
        String authSshKeyPath,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {
    }

    public record SyncStateDto(
        String id,
        String repositoryId,
        com.company.intelligentdiagnosis.agent.infrastructure.repository.state.SyncStatus status,
        java.time.Instant startedAt,
        java.time.Instant completedAt,
        String latestCommit,
        String previousCommit,
        Integer changedFiles,
        String errorMessage,
        com.company.intelligentdiagnosis.agent.infrastructure.repository.state.TriggerType triggerType,
        String triggeredBy,
        java.time.Instant createdAt
    ) {
    }

    public record CreateRepositoryRequest(
        String name,
        String displayName,
        RepositoryType type,
        String url,
        String branch,
        String localPath,
        boolean enabled,
        AuthType authType,
        String authToken,
        String authUsername,
        String authPassword,
        String authSshKeyPath
    ) {
    }

    public record UpdateRepositoryRequest(
        String displayName,
        RepositoryType type,
        String url,
        String branch,
        String localPath,
        boolean enabled,
        AuthType authType,
        String authToken,
        String authUsername,
        String authPassword,
        String authSshKeyPath
    ) {
    }
}
