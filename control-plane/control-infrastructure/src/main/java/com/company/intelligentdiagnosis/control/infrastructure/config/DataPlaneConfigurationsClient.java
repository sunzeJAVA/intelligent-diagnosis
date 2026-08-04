package com.company.intelligentdiagnosis.control.infrastructure.config;

import com.company.intelligentdiagnosis.control.infrastructure.metrics.DataPlaneMetricsProperties;
import com.company.intelligentdiagnosis.control.infrastructure.security.ServiceAccountTokenProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

/**
 * 数据平面配置拉取客户端
 * <p>
 * 从数据平面 {@code /api/data/configurations} 拉取 LLM、RAG 等配置项。
 * 数据平面不可用时返回空列表，不影响控制平面配置展示。
 */
@Component
public class DataPlaneConfigurationsClient {

    private static final Logger log = LoggerFactory.getLogger(DataPlaneConfigurationsClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;

    public DataPlaneConfigurationsClient(DataPlaneMetricsProperties properties,
                                         ServiceAccountTokenProvider tokenProvider) {
        this.restClient = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestInterceptor((request, body, execution) -> {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getToken());
                return execution.execute(request, body);
            })
            .build();
    }

    /**
     * 拉取数据平面配置项列表
     *
     * @return 配置项列表，调用失败返回空列表
     */
    public List<SystemConfigurationService.ConfigurationItem> fetchConfigurations() {
        try {
            String json = restClient.get()
                .uri("/api/data/configurations")
                .retrieve()
                .body(String.class);
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            return OBJECT_MAPPER.readValue(json,
                new TypeReference<List<SystemConfigurationService.ConfigurationItem>>() {});
        } catch (RestClientException e) {
            log.debug("Data-plane configurations endpoint not available: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to parse data-plane configurations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
