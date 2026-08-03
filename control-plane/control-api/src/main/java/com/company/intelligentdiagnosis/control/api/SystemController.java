package com.company.intelligentdiagnosis.control.api;

import com.company.intelligentdiagnosis.control.infrastructure.health.HealthCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统状态接口
 * 为前端侧边栏等公共组件提供基础设施健康状态
 */
@RestController
@RequestMapping("/api/control/system")
public class SystemController {

    private static final List<String> SIDEBAR_SERVICES = List.of("Qdrant", "Neo4j", "Temporal");

    private final HealthCheckService healthCheckService;

    public SystemController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    /**
     * 获取前端需要展示的基础设施健康状态
     *
     * @return 健康状态列表
     */
    @GetMapping("/health")
    public ResponseEntity<List<HealthCheckService.InfrastructureHealth>> health() {
        List<HealthCheckService.InfrastructureHealth> healthList = healthCheckService.checkAll().stream()
            .filter(h -> SIDEBAR_SERVICES.contains(h.name()))
            .toList();
        return ResponseEntity.ok(healthList);
    }
}
