package com.company.intelligentdiagnosis.control.infrastructure.security;

import com.company.intelligentdiagnosis.control.domain.audit.AuditAction;
import com.company.intelligentdiagnosis.control.infrastructure.audit.AuditEntryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 安全指标定时任务
 * 更新锁定账户 Gauge、检测暴力破解异常
 */
@Component
public class SecurityMetricsScheduler {

    private static final Logger log = LoggerFactory.getLogger(SecurityMetricsScheduler.class);

    private static final long LOCKED_ACCOUNTS_CHECK_INTERVAL_MS = 60_000;
    private static final long ANOMALY_CHECK_INTERVAL_MS = 60_000;
    private static final long ANOMALY_WINDOW_MINUTES = 5;
    private static final long ANOMALY_THRESHOLD = 10;

    private final UserJpaRepository userJpaRepository;
    private final AuditEntryRepository auditEntryRepository;
    private final Counter bruteForceAnomalyCounter;
    private final AtomicLong lockedAccountsGaugeValue = new AtomicLong(0);

    public SecurityMetricsScheduler(UserJpaRepository userJpaRepository,
                                    AuditEntryRepository auditEntryRepository,
                                    MeterRegistry meterRegistry) {
        this.userJpaRepository = userJpaRepository;
        this.auditEntryRepository = auditEntryRepository;
        this.bruteForceAnomalyCounter = Counter.builder("security_anomaly_brute_force_total")
            .description("Total number of detected brute-force login anomalies")
            .register(meterRegistry);

        Gauge.builder("security_locked_accounts", lockedAccountsGaugeValue, AtomicLong::get)
            .description("Number of currently locked accounts")
            .register(meterRegistry);
    }

    /**
     * 定时更新锁定账户数量
     */
    @Scheduled(fixedRate = LOCKED_ACCOUNTS_CHECK_INTERVAL_MS)
    public void updateLockedAccountsGauge() {
        long count = userJpaRepository.countByLockedUntilAfter(Instant.now());
        lockedAccountsGaugeValue.set(count);
        log.debug("Locked accounts count: {}", count);
    }

    /**
     * 定时检测暴力破解异常
     */
    @Scheduled(fixedRate = ANOMALY_CHECK_INTERVAL_MS)
    public void detectBruteForceAnomaly() {
        Instant since = Instant.now().minus(ANOMALY_WINDOW_MINUTES, ChronoUnit.MINUTES);
        long failureCount = auditEntryRepository.countByActionAndTimestampAfter(AuditAction.LOGIN_FAILURE, since);

        if (failureCount >= ANOMALY_THRESHOLD) {
            log.warn("SECURITY_ANOMALY: {} login failures in the last {} minutes, possible brute-force attack",
                failureCount, ANOMALY_WINDOW_MINUTES);
            bruteForceAnomalyCounter.increment();
        }
    }
}
