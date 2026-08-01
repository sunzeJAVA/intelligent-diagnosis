package com.company.intelligentdiagnosis.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全配置属性
 */
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 账户锁定配置
     */
    private Lockout lockout = new Lockout();

    public Lockout getLockout() {
        return lockout;
    }

    public void setLockout(Lockout lockout) {
        this.lockout = lockout;
    }

    public static class Lockout {

        /**
         * 连续登录失败多少次后锁定账户，默认 5 次
         */
        private int maxAttempts = 5;

        /**
         * 锁定持续时间（分钟），默认 30 分钟
         */
        private int durationMinutes = 30;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
        }
    }
}
