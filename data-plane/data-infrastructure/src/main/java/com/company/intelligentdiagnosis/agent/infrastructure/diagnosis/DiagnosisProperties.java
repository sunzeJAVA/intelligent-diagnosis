package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "diagnosis")
public class DiagnosisProperties {

    private int topK = 10;

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }
}
