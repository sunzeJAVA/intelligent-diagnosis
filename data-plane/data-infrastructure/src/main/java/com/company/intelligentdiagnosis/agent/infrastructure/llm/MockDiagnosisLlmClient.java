package com.company.intelligentdiagnosis.agent.infrastructure.llm;

import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock LLM 客户端实现
 * 返回固定的模拟响应，用于开发和测试环境
 */
public class MockDiagnosisLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockDiagnosisLlmClient.class);

    private static final String MOCK_RESPONSE = """
        {
          "summary": "当前使用 Mock LLM，未接入真实模型",
          "rootCause": "llm.provider=mock，系统仅返回固定诊断结果用于流程验证。",
          "suggestions": [
            "设置环境变量 LLM_PROVIDER=openai 并配置 LLM_API_KEY",
            "重启 data-plane 以使用真实 LLM 生成诊断"
          ]
        }
        """;

    @Override
    public LlmCompletion complete(String systemPrompt, String userPrompt) {
        log.info("Mock LLM called; returning fixed response");
        // Mock 响应视为降级，提示用户未接入真实模型
        return LlmCompletion.degraded(MOCK_RESPONSE);
    }
}
