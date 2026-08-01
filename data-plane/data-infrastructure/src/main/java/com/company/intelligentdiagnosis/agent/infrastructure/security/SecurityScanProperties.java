package com.company.intelligentdiagnosis.agent.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全扫描配置属性
 */
@ConfigurationProperties(prefix = "security.scan")
public class SecurityScanProperties {

    /**
     * 是否启用安全扫描
     */
    private boolean enabled = true;

    /**
     * 发现 HIGH 级别问题时是否阻断工作流
     */
    private boolean blockOnHigh = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBlockOnHigh() {
        return blockOnHigh;
    }

    public void setBlockOnHigh(boolean blockOnHigh) {
        this.blockOnHigh = blockOnHigh;
    }
}
