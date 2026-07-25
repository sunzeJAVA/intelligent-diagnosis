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

/**
 * 仓库管理 API 控制器
 * 提供代码仓库的 CRUD 和同步操作接口
 */
@RestController
@RequestMapping("/api/data/repositories")
public class RepositoryController {

    private final RepositoryApplicationService repositoryApplicationService;

    /**
     * 创建实例
     *
     * @param repositoryApplicationService 仓库应用服务
     */
    public RepositoryController(RepositoryApplicationService repositoryApplicationService) {
        this.repositoryApplicationService = repositoryApplicationService;
    }

    /**
     * 查询仓库列表
     *
     * @return 仓库列表
     */
    @GetMapping
    public ResponseEntity<List<RepositoryDto>> listRepositories() {
        List<RepositoryDto> result = repositoryApplicationService.listRepositories().stream()
            .map(this::toDto)
            .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * 查询单个仓库
     *
     * @param id 仓库 ID
     * @return 仓库详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<RepositoryDto> getRepository(@PathVariable String id) {
        return ResponseEntity.ok(toDto(repositoryApplicationService.getRepository(id)));
    }

    /**
     * 创建仓库配置
     *
     * @param request 创建请求
     * @return 创建的仓库详情
     */
    @PostMapping
    public ResponseEntity<RepositoryDto> createRepository(@RequestBody CreateRepositoryRequest request) {
        RepositoryConfigEntity entity = repositoryApplicationService.createRepository(toCommand(request));
        return ResponseEntity.created(URI.create("/api/data/repositories/" + entity.getId()))
            .body(toDto(entity));
    }

    /**
     * 更新仓库配置
     *
     * @param id      仓库 ID
     * @param request 更新请求
     * @return 更新后的仓库详情
     */
    @PutMapping("/{id}")
    public ResponseEntity<RepositoryDto> updateRepository(@PathVariable String id,
                                                          @RequestBody UpdateRepositoryRequest request) {
        return ResponseEntity.ok(toDto(repositoryApplicationService.updateRepository(id, toCommand(request))));
    }

    /**
     * 删除仓库配置
     *
     * @param id 仓库 ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepository(@PathVariable String id) {
        repositoryApplicationService.deleteRepository(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 手动触发仓库同步
     *
     * @param id 仓库 ID
     * @return 同步状态
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<SyncStateDto> syncRepository(@PathVariable String id) {
        return ResponseEntity.ok(toDto(repositoryApplicationService.syncRepository(id, "admin")));
    }

    /**
     * 查询仓库同步历史
     *
     * @param id 仓库 ID
     * @return 同步历史列表
     */
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
