package com.company.intelligentdiagnosis.agent.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer 指标配置
 */
@Configuration
public class MetricsConfig {

    public static final String DIAGNOSIS_COUNTER_NAME = "diagnosis_total";

    /**
     * 累计诊断次数计数器
     */
    @Bean
    public Counter diagnosisCounter(MeterRegistry meterRegistry) {
        return Counter.builder(DIAGNOSIS_COUNTER_NAME)
            .description("Total number of diagnosis requests")
            .register(meterRegistry);
    }
}
