package com.company.intelligentdiagnosis.agent.boot;

import com.company.intelligentdiagnosis.agent.infrastructure.diagnosis.DiagnosisProperties;
import com.company.intelligentdiagnosis.agent.infrastructure.llm.LlmProperties;
import com.company.intelligentdiagnosis.agent.infrastructure.parse.ParseWorkerProperties;
import com.company.intelligentdiagnosis.agent.infrastructure.vector.EmbeddingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Data Plane 启动类
 * 智能诊断系统的数据处理服务入口
 */
@SpringBootApplication(scanBasePackages = "com.company.intelligentdiagnosis.agent")
@EnableJpaRepositories(basePackages = "com.company.intelligentdiagnosis.agent.infrastructure")
@EntityScan(basePackages = "com.company.intelligentdiagnosis.agent.infrastructure")
@EnableConfigurationProperties({
    ParseWorkerProperties.class,
    LlmProperties.class,
    EmbeddingProperties.class,
    DiagnosisProperties.class
})
@EnableScheduling
public class DataPlaneApplication {

    /**
     * 启动应用
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DataPlaneApplication.class, args);
    }
}
