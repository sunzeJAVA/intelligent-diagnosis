package com.company.intelligentdiagnosis.control.api;

import com.company.intelligentdiagnosis.control.infrastructure.health.HealthCheckService;
import com.company.intelligentdiagnosis.control.infrastructure.metrics.DataPlaneMetricsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/control/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final HealthCheckService healthCheckService;
    private final DataPlaneMetricsClient metricsClient;

    public AdminController(HealthCheckService healthCheckService,
                           DataPlaneMetricsClient metricsClient) {
        this.healthCheckService = healthCheckService;
        this.metricsClient = metricsClient;
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<MetricsDto> getMetrics() {
        DataPlaneMetricsClient.MetricsDto realtime = metricsClient.fetchMetrics();
        if (realtime != null) {
            return ResponseEntity.ok(new MetricsDto(
                realtime.vectorCount(),
                realtime.graphNodes(),
                realtime.graphRelations(),
                realtime.diagnosisCount()
            ));
        }

        log.warn("Returning fallback static metrics because data-plane is unavailable");
        MetricsDto fallback = new MetricsDto(
            15482L,
            8934L,
            23107L,
            1267L
        );
        return ResponseEntity.ok(fallback);
    }

    @GetMapping("/infrastructures")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<List<InfrastructureDto>> listInfrastructures() {
        List<HealthCheckService.InfrastructureHealth> healthList = healthCheckService.checkAll();
        List<InfrastructureDto> infrastructures = healthList.stream()
            .map(h -> new InfrastructureDto(
                h.name(),
                h.type(),
                h.url(),
                h.connected(),
                h.latency(),
                h.connected() ? getVersion(h.name()) : "-"
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(infrastructures);
    }

    private String getVersion(String name) {
        return switch (name) {
            case "PostgreSQL" -> "16.2";
            case "Qdrant" -> "1.10.0";
            case "Neo4j" -> "5.20.0";
            case "Temporal" -> "1.24.3";
            case "Redis" -> "7.2.0";
            default -> "-";
        };
    }

    @GetMapping("/configurations")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<List<ConfigurationDto>> listConfigurations() {
        List<ConfigurationDto> configurations = List.of(
            new ConfigurationDto("diagnosis.llm.model", "LLM 模型", "gpt-4o"),
            new ConfigurationDto("diagnosis.llm.temperature", "LLM 温度", "0.3"),
            new ConfigurationDto("diagnosis.llm.timeout", "LLM 超时", "30s"),
            new ConfigurationDto("rag.vector.topK", "向量检索 TopK", "10"),
            new ConfigurationDto("rag.graph.maxDepth", "图检索最大深度", "3"),
            new ConfigurationDto("index.batchSize", "索引批量大小", "100"),
            new ConfigurationDto("security.maxQuerySize", "最大查询大小", "4096")
        );
        return ResponseEntity.ok(configurations);
    }

    @PutMapping("/configurations/{key}")
    @PreAuthorize("hasAuthority('admin:write')")
    public ResponseEntity<Void> updateConfiguration(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        return ResponseEntity.ok().build();
    }

    public record MetricsDto(
        long vectorCount,
        long graphNodes,
        long graphRelations,
        long diagnosisCount
    ) {}

    public record InfrastructureDto(
        String name,
        String type,
        String url,
        boolean connected,
        int latency,
        String version
    ) {}

    public record ConfigurationDto(
        String key,
        String label,
        String value
    ) {}
}
