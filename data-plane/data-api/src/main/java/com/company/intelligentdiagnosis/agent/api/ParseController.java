package com.company.intelligentdiagnosis.agent.api;

import com.company.intelligentdiagnosis.agent.application.ParseApplicationService;
import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.parse.ParseCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/data/parse")
public class ParseController {

    private final ParseApplicationService parseApplicationService;

    public ParseController(ParseApplicationService parseApplicationService) {
        this.parseApplicationService = parseApplicationService;
    }

    @PostMapping
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
