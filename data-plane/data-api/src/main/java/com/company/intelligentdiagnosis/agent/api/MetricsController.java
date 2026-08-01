package com.company.intelligentdiagnosis.agent.api;

import com.company.intelligentdiagnosis.agent.infrastructure.metrics.DataPlaneMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据平面指标 API
 */
@RestController
@RequestMapping("/api/data/metrics")
public class MetricsController {

    private final DataPlaneMetricsService metricsService;

    public MetricsController(DataPlaneMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<DataPlaneMetricsService.MetricsDto> getMetrics() {
        return ResponseEntity.ok(metricsService.collectMetrics());
    }
}
