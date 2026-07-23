package com.company.intelligentdiagnosis.control.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.company.intelligentdiagnosis.control")
@EntityScan("com.company.intelligentdiagnosis.control.domain")
@EnableJpaRepositories("com.company.intelligentdiagnosis.control.infrastructure")
public class ControlPlaneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ControlPlaneApplication.class, args);
    }
}
