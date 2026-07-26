package com.company.intelligentdiagnosis.control.infrastructure.workflow;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalClientConfig {

    @Value("${temporal.service-client.target:localhost:7233}")
    private String temporalTarget;

    @Value("${temporal.enabled:true}")
    private boolean temporalEnabled;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        if (!temporalEnabled) {
            return null;
        }
        try {
            return WorkflowServiceStubs.newInstance(
                WorkflowServiceStubsOptions.newBuilder()
                    .setTarget(temporalTarget)
                    .build()
            );
        } catch (Exception e) {
            return null;
        }
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        if (serviceStubs == null) {
            return null;
        }
        return WorkflowClient.newInstance(serviceStubs);
    }
}
