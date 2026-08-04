package com.company.intelligentdiagnosis.control.api;

import com.company.intelligentdiagnosis.control.infrastructure.config.SystemConfigurationService;
import com.company.intelligentdiagnosis.control.infrastructure.health.HealthCheckService;
import com.company.intelligentdiagnosis.control.infrastructure.health.ParseWorkerHealthChecker;
import com.company.intelligentdiagnosis.control.infrastructure.metrics.DataPlaneMetricsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/control/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final HealthCheckService healthCheckService;
    private final DataPlaneMetricsClient metricsClient;
    private final ParseWorkerHealthChecker parseWorkerHealthChecker;
    private final SystemConfigurationService configurationService;

    public AdminController(HealthCheckService healthCheckService,
                           DataPlaneMetricsClient metricsClient,
                           ParseWorkerHealthChecker parseWorkerHealthChecker,
                           SystemConfigurationService configurationService) {
        this.healthCheckService = healthCheckService;
        this.metricsClient = metricsClient;
        this.parseWorkerHealthChecker = parseWorkerHealthChecker;
        this.configurationService = configurationService;
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

        // 数据平面不可用时返回零值，前端据此展示"暂时不可用"
        log.warn("Data-plane unavailable, returning zero metrics");
        return ResponseEntity.ok(new MetricsDto(0, 0, 0, 0));
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
                h.version()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(infrastructures);
    }

    @GetMapping("/parse-workers")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<List<ParseWorkerDto>> listParseWorkers() {
        return ResponseEntity.ok(parseWorkerHealthChecker.checkAll().stream()
            .map(h -> new ParseWorkerDto(h.name(), h.language(), h.address(), h.healthy(), h.latency()))
            .collect(Collectors.toList()));
    }

    @GetMapping("/configurations")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<List<ConfigurationDto>> listConfigurations() {
        return ResponseEntity.ok(configurationService.listConfigurations().stream()
            .map(c -> new ConfigurationDto(c.key(), c.label(), c.value(), c.source()))
            .collect(Collectors.toList()));
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

    public record ParseWorkerDto(
        String name,
        String language,
        String address,
        boolean healthy,
        int latency
    ) {}

    public record ConfigurationDto(
        String key,
        String label,
        String value,
        String source
    ) {}
}
