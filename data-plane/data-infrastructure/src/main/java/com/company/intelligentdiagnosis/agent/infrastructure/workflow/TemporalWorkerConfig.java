package com.company.intelligentdiagnosis.agent.infrastructure.workflow;

import com.company.intelligentdiagnosis.agent.infrastructure.workflow.activity.IndexUpdateActivitiesImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

@Configuration
public class TemporalWorkerConfig {

    private static final Logger log = LoggerFactory.getLogger(TemporalWorkerConfig.class);

    private static final String TASK_QUEUE = "index-update-task-queue";

    @Value("${temporal.host:localhost}")
    private String temporalHost;

    @Value("${temporal.port:7233}")
    private int temporalPort;

    private WorkerFactory workerFactory;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newInstance(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalHost + ":" + temporalPort)
                .build()
        );
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(serviceStubs);
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient client) {
        workerFactory = WorkerFactory.newInstance(client);
        return workerFactory;
    }

    @Bean
    public Worker worker(WorkerFactory factory, IndexUpdateActivitiesImpl activities) {
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(IndexUpdateWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        factory.start();
        log.info("Temporal worker started for task queue: {}", TASK_QUEUE);
        return worker;
    }

    @PreDestroy
    public void shutdown() {
        if (workerFactory != null) {
            log.info("Shutting down Temporal worker");
            workerFactory.shutdown();
        }
    }
}
