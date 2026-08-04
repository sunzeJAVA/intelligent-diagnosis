package com.company.intelligentdiagnosis.control.infrastructure.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务（只读）
 * <p>
 * 从 Spring {@link Environment} 动态读取控制平面配置项，
 * 同时通过 {@link DataPlaneConfigurationsClient} 拉取数据平面配置（LLM、RAG 等）。
 * 修改配置请编辑 application.yml 后重启服务。
 */
@Service
public class SystemConfigurationService {

    private final Environment environment;
    private final DataPlaneConfigurationsClient dataPlaneConfigurationsClient;

    /**
     * 控制平面配置项定义：key → 展示标签
     */
    private static final Map<String, String> CONTROL_PLANE_KEYS = new LinkedHashMap<>() {{
        put("security.jwt.issuer", "JWT 签发者");
        put("security.jwt.expiration-ms", "JWT 有效期 (ms)");
        put("security.lockout.max-attempts", "账户锁定阈值");
        put("security.lockout.duration-minutes", "锁定时长 (分钟)");
        put("temporal.service-client.target", "Temporal 地址");
        put("management.tracing.sampling.probability", "Trace 采样率");
    }};

    public SystemConfigurationService(Environment environment,
                                      DataPlaneConfigurationsClient dataPlaneConfigurationsClient) {
        this.environment = environment;
        this.dataPlaneConfigurationsClient = dataPlaneConfigurationsClient;
    }

    /**
     * 列出所有系统配置（控制平面 + 数据平面）
     */
    public List<ConfigurationItem> listConfigurations() {
        List<ConfigurationItem> result = new ArrayList<>();

        // 控制平面配置
        CONTROL_PLANE_KEYS.forEach((key, label) -> {
            String value = environment.getProperty(key, "—");
            result.add(new ConfigurationItem(key, label, value, "control-plane"));
        });

        // 数据平面配置（LLM、RAG 等）
        List<ConfigurationItem> dataPlaneConfigs = dataPlaneConfigurationsClient.fetchConfigurations();
        if (dataPlaneConfigs != null) {
            result.addAll(dataPlaneConfigs);
        }

        return result;
    }

    public record ConfigurationItem(
        String key,
        String label,
        String value,
        String source
    ) {}
}
