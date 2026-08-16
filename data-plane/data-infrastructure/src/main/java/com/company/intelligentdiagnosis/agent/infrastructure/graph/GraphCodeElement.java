package com.company.intelligentdiagnosis.agent.infrastructure.graph;

/**
 * 从 Neo4j 图数据库中查询出的代码元素节点
 * <p>
 * 用于图召回场景，包含节点的核心属性和关系信息。
 *
 * @param id            元素唯一标识
 * @param kind          元素类型（CLASS / METHOD / INTERFACE 等）
 * @param name          元素名称
 * @param qualifiedName 元素全限定名
 * @param filePath      文件路径
 * @param startLine     起始行号
 * @param endLine       结束行号
 * @param sourceCode    源代码内容
 * @param documentation 文档注释
 */
public record GraphCodeElement(
    String id,
    String kind,
    String name,
    String qualifiedName,
    String filePath,
    int startLine,
    int endLine,
    String sourceCode,
    String documentation
) {
}
