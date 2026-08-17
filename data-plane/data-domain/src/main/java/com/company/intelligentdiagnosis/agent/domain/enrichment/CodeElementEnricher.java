package com.company.intelligentdiagnosis.agent.domain.enrichment;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;

import java.util.List;

/**
 * 代码元素富化器
 * <p>
 * 在解析完成后、索引写入前对代码元素进行语义增强，例如：
 * <ul>
 *   <li>生成中文/英文摘要</li>
 *   <li>提取关键词、调用意图</li>
 * </ul>
 */
public interface CodeElementEnricher {

    /**
     * 富化代码元素列表
     *
     * @param elements 原始代码元素
     * @return 富化后的代码元素
     */
    List<CodeElement> enrich(List<CodeElement> elements);
}
