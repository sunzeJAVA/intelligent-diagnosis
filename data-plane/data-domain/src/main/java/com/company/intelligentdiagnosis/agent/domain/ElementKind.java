package com.company.intelligentdiagnosis.agent.domain;

/**
 * 代码元素类型枚举
 * 定义从源代码中解析出的各种元素类型
 */
public enum ElementKind {
    UNKNOWN,
    NAMESPACE,
    CLASS,
    INTERFACE,
    METHOD,
    FUNCTION,
    PROPERTY,
    FIELD,
    VARIABLE
}
