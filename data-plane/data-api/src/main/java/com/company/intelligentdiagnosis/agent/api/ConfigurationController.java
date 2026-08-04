package com.company.intelligentdiagnosis.agent.api;

import com.company.intelligentdiagnosis.agent.infrastructure.config.DataPlaneConfigurationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据平面配置 API
 * <p>
 * 暴露 LLM、RAG、索引等配置项供控制平面聚合展示。
 */
@RestController
@RequestMapping("/api/data/configurations")
public class ConfigurationController {

    private final DataPlaneConfigurationService configurationService;

    public ConfigurationController(DataPlaneConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<List<DataPlaneConfigurationService.ConfigurationItem>> listConfigurations() {
        return ResponseEntity.ok(configurationService.listConfigurations());
    }
}
