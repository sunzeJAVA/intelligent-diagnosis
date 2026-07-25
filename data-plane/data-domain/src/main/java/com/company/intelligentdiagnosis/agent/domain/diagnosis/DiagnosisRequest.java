package com.company.intelligentdiagnosis.agent.domain.diagnosis;

/**
 * 诊断请求
 * 包含诊断所需的所有输入信息
 *
 * @param query     用户查询的问题描述
 * @param errorInfo 错误信息或异常堆栈
 * @param service   相关服务名称（可选）
 * @param userId    用户 ID（可选）
 * @param tenantId  租户 ID（可选）
 */
public record DiagnosisRequest(
    String query,
    String errorInfo,
    String service,
    String userId,
    String tenantId
) {
}
