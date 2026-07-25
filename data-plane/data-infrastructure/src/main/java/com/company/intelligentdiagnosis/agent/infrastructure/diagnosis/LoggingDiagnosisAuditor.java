package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisAuditor;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 日志诊断审计器
 * 通过日志记录诊断请求和响应信息
 */
@Component
public class LoggingDiagnosisAuditor implements DiagnosisAuditor {

    private static final Logger log = LoggerFactory.getLogger(LoggingDiagnosisAuditor.class);

    @Override
    public void record(DiagnosisRequest request, DiagnosisResponse response, long durationMillis) {
        log.info("Diagnosis completed for service {} in {}ms: summary={}",
            request.service(), durationMillis, response.summary());
    }
}
