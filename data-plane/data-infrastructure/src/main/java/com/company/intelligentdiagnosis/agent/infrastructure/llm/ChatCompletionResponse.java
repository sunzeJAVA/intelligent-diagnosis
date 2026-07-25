package com.company.intelligentdiagnosis.agent.infrastructure.llm;

import java.util.List;

/**
 * 聊天补全响应
 * OpenAI 风格的 API 响应结构
 */
public record ChatCompletionResponse(
    /**
     * 生成的选项列表
     */
    List<Choice> choices
) {
    /**
     * 生成选项
     */
    public record Choice(
        /**
         * 选项索引
         */
        int index,
        /**
         * 消息内容
         */
        Message message,
        /**
         * 结束原因
         */
        String finishReason
    ) {
    }

    /**
     * 消息记录
     */
    public record Message(
        /**
         * 角色：assistant
         */
        String role,
        /**
         * 消息内容
         */
        String content
    ) {
    }
}
