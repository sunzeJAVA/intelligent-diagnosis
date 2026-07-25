package com.company.intelligentdiagnosis.agent.domain;

/**
 * 关系类型枚举
 * 定义代码元素之间的各种关系类型
 */
public enum RelationKind {
    CALLS,
    IMPLEMENTS,
    EXTENDS,
    DEPENDS_ON,
    CONTAINS
}
