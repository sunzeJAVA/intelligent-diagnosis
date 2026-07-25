package com.company.intelligentdiagnosis.agent.infrastructure.llm;

import java.util.List;

/**
 * 聊天补全请求
 * OpenAI 风格的 API 请求结构
 */
public record ChatCompletionRequest(
    /**
     * 模型名称
     */
    String model,
    /**
     * 消息列表
     */
    List<Message> messages,
    /**
     * 温度参数，控制生成的随机性
     */
    Double temperature,
    /**
     * 最大生成 token 数
     */
    Integer maxTokens
) {
    /**
     * 消息记录
     */
    public record Message(
        /**
         * 角色：system/user/assistant
         */
        String role,
        /**
         * 消息内容
         */
        String content
    ) {
    }
}
