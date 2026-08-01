package com.company.intelligentdiagnosis.control.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.company.intelligentdiagnosis.control")
@EnableJpaRepositories(basePackages = "com.company.intelligentdiagnosis.control.infrastructure")
@EntityScan(basePackages = "com.company.intelligentdiagnosis.control.infrastructure")
@ConfigurationPropertiesScan(basePackages = "com.company.intelligentdiagnosis.control")
public class ControlPlaneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ControlPlaneApplication.class, args);
    }
}
