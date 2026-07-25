package com.company.intelligentdiagnosis.agent.infrastructure.vector;

/**
 * 嵌入生成器接口
 * 将文本转换为向量表示
 */
public interface EmbeddingGenerator {

    /**
     * 将文本转换为向量
     *
     * @param text 输入文本
     * @return 向量表示
     */
    float[] embed(String text);

    /**
     * 获取向量维度
     *
     * @return 向量维度
     */
    int dimension();
}
