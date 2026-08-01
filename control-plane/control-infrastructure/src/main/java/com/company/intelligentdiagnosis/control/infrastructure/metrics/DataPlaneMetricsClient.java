package com.company.intelligentdiagnosis.control.infrastructure.metrics;

import com.company.intelligentdiagnosis.control.infrastructure.security.ServiceAccountTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 数据平面指标客户端
 */
@Component
public class DataPlaneMetricsClient {

    private static final Logger log = LoggerFactory.getLogger(DataPlaneMetricsClient.class);

    private final RestClient restClient;

    public DataPlaneMetricsClient(DataPlaneMetricsProperties properties,
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
     * 从数据平面获取实时指标
     *
     * @return 指标数据，若调用失败返回 null
     */
    public MetricsDto fetchMetrics() {
        try {
            return restClient.get()
                .uri("/api/data/metrics")
                .retrieve()
                .body(MetricsDto.class);
        } catch (RestClientException e) {
            log.warn("Failed to fetch metrics from data-plane: {}", e.getMessage());
            return null;
        }
    }

    public record MetricsDto(
        long vectorCount,
        long graphNodes,
        long graphRelations,
        long diagnosisCount
    ) {}
}
