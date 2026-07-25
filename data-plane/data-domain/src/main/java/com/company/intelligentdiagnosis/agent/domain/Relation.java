package com.company.intelligentdiagnosis.agent.domain;

/**
 * 元素关系
 * 表示两个代码元素之间的关系
 *
 * @param kind     关系类型
 * @param targetId 目标元素 ID
 */
public record Relation(RelationKind kind, String targetId) {
}
