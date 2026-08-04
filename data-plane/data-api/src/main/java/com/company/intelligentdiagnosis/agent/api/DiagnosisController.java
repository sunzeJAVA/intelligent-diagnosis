package com.company.intelligentdiagnosis.agent.api;

import com.company.intelligentdiagnosis.agent.application.DiagnosisApplicationService;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeSnippet;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/data/diagnosis")
public class DiagnosisController {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisController.class);

    private final DiagnosisApplicationService diagnosisApplicationService;

    public DiagnosisController(DiagnosisApplicationService diagnosisApplicationService) {
        this.diagnosisApplicationService = diagnosisApplicationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('diagnosis:read')")
    @RateLimiter(name = "diagnosis-api")
    public ResponseEntity<ApiResponse<DiagnosisResponseDto>> diagnose(@RequestBody DiagnosisRequestDto request) {
        try {
            DiagnosisRequest domainRequest = new DiagnosisRequest(
                request.query(),
                request.errorInfo(),
                request.service(),
                null,
                null
            );
            DiagnosisResponse result = diagnosisApplicationService.diagnose(domainRequest);
            return ResponseEntity.ok(ApiResponse.ok(toDto(result)));
        } catch (Exception e) {
            log.error("Diagnosis failed for service {}: {}", request.service(), e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("诊断失败: " + e.getMessage()));
        }
    }

    private DiagnosisResponseDto toDto(DiagnosisResponse response) {
        DiagnosisIntentDto intentDto = response.intent() != null ? toDto(response.intent()) : null;
        return new DiagnosisResponseDto(
            response.summary(),
            response.rootCause(),
            response.suggestions(),
            response.relatedCode().stream()
                .map(this::toDto)
                .toList(),
            intentDto
        );
    }

    private CodeSnippetDto toDto(CodeSnippet snippet) {
        return new CodeSnippetDto(
            snippet.filePath(),
            snippet.startLine(),
            snippet.endLine(),
            snippet.content()
        );
    }

    private DiagnosisIntentDto toDto(com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent intent) {
        return new DiagnosisIntentDto(
            intent.type().name(),
            intent.type().getDisplayName(),
            intent.confidence(),
            intent.entities(),
            intent.enhancedQuery()
        );
    }

    public record DiagnosisRequestDto(
        String query,
        String errorInfo,
        String service
    ) {}

    public record DiagnosisResponseDto(
        String summary,
        String rootCause,
        List<String> suggestions,
        List<CodeSnippetDto> relatedCode,
        DiagnosisIntentDto intent
    ) {}

    public record CodeSnippetDto(
        String filePath,
        int startLine,
        int endLine,
        String content
    ) {}

    /**
     * 意图识别结果 DTO
     *
     * @param type          意图类型枚举名（大写）
     * @param displayName   意图类型中文名
     * @param confidence    置信度 0.0-1.0
     * @param entities      关键实体列表
     * @param enhancedQuery 增强后的检索关键词
     */
    public record DiagnosisIntentDto(
        String type,
        String displayName,
        double confidence,
        List<String> entities,
        String enhancedQuery
    ) {}
}
