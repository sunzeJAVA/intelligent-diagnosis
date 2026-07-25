package com.company.intelligentdiagnosis.agent.domain.diagnosis;

/**
 * 代码片段
 * 表示从代码库中检索到的相关代码片段
 *
 * @param filePath  文件路径
 * @param startLine 起始行号
 * @param endLine   结束行号
 * @param content   代码内容
 */
public record CodeSnippet(
    String filePath,
    int startLine,
    int endLine,
    String content
) {
}
