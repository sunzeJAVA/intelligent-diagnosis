package com.company.intelligentdiagnosis.agent.api;

import com.company.intelligentdiagnosis.agent.application.ParseApplicationService;
import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.parse.ParseCommand;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 代码解析 API 控制器
 * 提供代码解析和索引操作接口
 */
@RestController
@RequestMapping("/api/data/parse")
public class ParseController {

    private final ParseApplicationService parseApplicationService;

    /**
     * 创建实例
     *
     * @param parseApplicationService 解析应用服务
     */
    public ParseController(ParseApplicationService parseApplicationService) {
        this.parseApplicationService = parseApplicationService;
    }

    /**
     * 解析代码并建立索引
     *
     * @param request 解析请求
     * @return 解析出的代码元素列表
     */
    @PostMapping
    @RateLimiter(name = "parse-api")
    public ResponseEntity<List<CodeElementDto>> parse(@RequestBody ParseRequestDto request) {
        ParseCommand command = new ParseCommand(
            request.repository(),
            request.commitHash(),
            request.repoPath(),
            request.changedFiles(),
            request.language()
        );

        List<CodeElement> elements = parseApplicationService.parseAndIndex(command);
        return ResponseEntity.ok(elements.stream()
            .map(ParseController::toDto)
            .toList());
    }

    private static CodeElementDto toDto(CodeElement element) {
        return new CodeElementDto(
            element.id(),
            element.kind().name(),
            element.name(),
            element.qualifiedName(),
            element.filePath(),
            element.startLine(),
            element.endLine(),
            element.sourceCode(),
            element.documentation(),
            element.modifiers(),
            element.relations().stream()
                .map(r -> new RelationDto(r.kind().name(), r.targetId()))
                .toList()
        );
    }

    public record ParseRequestDto(
        String repository,
        String commitHash,
        String repoPath,
        List<String> changedFiles,
        String language
    ) {
    }

    public record CodeElementDto(
        String id,
        String kind,
        String name,
        String qualifiedName,
        String filePath,
        int startLine,
        int endLine,
        String sourceCode,
        String documentation,
        List<String> modifiers,
        List<RelationDto> relations
    ) {
    }

    public record RelationDto(String kind, String targetId) {
    }
}
