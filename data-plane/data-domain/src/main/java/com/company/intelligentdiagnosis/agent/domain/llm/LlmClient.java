package com.company.intelligentdiagnosis.agent.domain.llm;

/**
 * LLM 客户端接口
 * 定义与大语言模型交互的标准契约
 */
public interface LlmClient {

    /**
     * 完成文本生成
     *
     * @param systemPrompt 系统提示词，用于指导模型行为
     * @param userPrompt   用户输入的提示词
     * @return 模型生成的响应文本
     * @throws com.company.intelligentdiagnosis.agent.infrastructure.llm.LlmClientException 当调用失败时抛出
     */
    String complete(String systemPrompt, String userPrompt);
}
