package com.company.intelligentdiagnosis.control.infrastructure.health;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HealthCheckServiceTest {

    private final InfrastructureProperties properties = new InfrastructureProperties();
    private final DataSource dataSource = mock(DataSource.class);
    private final HealthCheckService service = new HealthCheckService(properties, dataSource);

    @Test
    void shouldReportRedisUnhealthyWhenPortIsUnavailable() {
        properties.setRedis(new InfrastructureProperties.Endpoint("127.0.0.1", 1));

        HealthCheckService.InfrastructureHealth health = service.checkRedis();

        assertThat(health.connected()).isFalse();
        assertThat(health.name()).isEqualTo("Redis");
    }

    @Test
    void shouldReportTemporalUnhealthyWhenPortIsUnavailable() {
        properties.setTemporal(new InfrastructureProperties.Endpoint("127.0.0.1", 1));

        HealthCheckService.InfrastructureHealth health = service.checkTemporal();

        assertThat(health.connected()).isFalse();
        assertThat(health.name()).isEqualTo("Temporal");
    }

    @Test
    void shouldReportQdrantUnhealthyWhenPortIsUnavailable() {
        properties.setQdrant(new InfrastructureProperties.Endpoint("127.0.0.1", 1));

        HealthCheckService.InfrastructureHealth health = service.checkQdrant();

        assertThat(health.connected()).isFalse();
        assertThat(health.name()).isEqualTo("Qdrant");
    }

    @Test
    void shouldReturnAllChecks() {
        assertThat(service.checkAll())
            .hasSize(5)
            .extracting(HealthCheckService.InfrastructureHealth::name)
            .containsExactly("PostgreSQL", "Qdrant", "Neo4j", "Temporal", "Redis");
    }
}
