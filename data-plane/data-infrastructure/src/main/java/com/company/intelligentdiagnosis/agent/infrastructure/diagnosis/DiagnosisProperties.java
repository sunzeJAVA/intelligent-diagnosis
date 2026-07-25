package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 诊断配置属性
 */
@ConfigurationProperties(prefix = "diagnosis")
public class DiagnosisProperties {

    /**
     * 向量搜索返回的结果数量
     */
    private int topK = 10;

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }
}
