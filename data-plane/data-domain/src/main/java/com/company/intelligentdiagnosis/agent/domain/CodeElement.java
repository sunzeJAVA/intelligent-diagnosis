package com.company.intelligentdiagnosis.agent.domain;

import java.util.List;
import java.util.Map;

/**
 * 代码元素
 * 表示从源代码中解析出的结构化元素（类、方法、字段等）
 *
 * @param id            元素唯一标识
 * @param kind          元素类型
 * @param name          元素名称
 * @param qualifiedName 元素全限定名
 * @param filePath      文件路径
 * @param startLine     起始行号
 * @param endLine       结束行号
 * @param sourceCode    源代码内容
 * @param documentation 文档注释
 * @param modifiers     修饰符列表（如 public、static）
 * @param relations     与其他元素的关系列表
 * @param metadata      元数据（语言特定的额外信息）
 */
public record CodeElement(
    String id,
    ElementKind kind,
    String name,
    String qualifiedName,
    String filePath,
    int startLine,
    int endLine,
    String sourceCode,
    String documentation,
    List<String> modifiers,
    List<Relation> relations,
    Map<String, String> metadata
) {

    /**
     * 创建一个新的 CodeElement，替换关系列表
     *
     * @param newRelations 新的关系列表
     * @return 新的 CodeElement 实例
     */
    public CodeElement withRelations(List<Relation> newRelations) {
        return new CodeElement(
            id, kind, name, qualifiedName, filePath, startLine, endLine,
            sourceCode, documentation, modifiers, newRelations, metadata
        );
    }
}
