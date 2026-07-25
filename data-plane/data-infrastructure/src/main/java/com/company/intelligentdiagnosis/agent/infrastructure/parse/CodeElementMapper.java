package com.company.intelligentdiagnosis.agent.infrastructure.parse;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.ElementKind;
import com.company.intelligentdiagnosis.agent.domain.Relation;
import com.company.intelligentdiagnosis.agent.domain.RelationKind;

import java.util.List;
import java.util.Map;

/**
 * 代码元素映射器
 * 将 gRPC 协议缓冲区对象转换为领域模型对象
 */
public final class CodeElementMapper {

    /**
     * 私有构造函数，防止实例化
     */
    private CodeElementMapper() {
    }

    /**
     * 将 gRPC 代码元素转换为领域模型
     *
     * @param proto gRPC 代码元素对象
     * @return 领域模型代码元素
     */
    public static CodeElement toDomain(com.company.intelligentdiagnosis.parse.CodeElement proto) {
        return new CodeElement(
            proto.getId(),
            toDomain(proto.getKind()),
            proto.getName(),
            proto.getQualifiedName(),
            proto.getFilePath(),
            proto.getStartLine(),
            proto.getEndLine(),
            proto.getSourceCode(),
            proto.getDocumentation(),
            List.copyOf(proto.getModifiersList()),
            proto.getRelationsList().stream()
                .map(CodeElementMapper::toDomain)
                .toList(),
            Map.copyOf(proto.getMetadataMap())
        );
    }

    /**
     * 将 gRPC 元素类型转换为领域模型元素类型
     *
     * @param kind gRPC 元素类型
     * @return 领域模型元素类型
     */
    private static ElementKind toDomain(com.company.intelligentdiagnosis.parse.ElementKind kind) {
        if (kind == null || kind == com.company.intelligentdiagnosis.parse.ElementKind.UNRECOGNIZED) {
            return ElementKind.UNKNOWN;
        }
        try {
            return ElementKind.valueOf(kind.name());
        } catch (IllegalArgumentException e) {
            return ElementKind.UNKNOWN;
        }
    }

    /**
     * 将 gRPC 关系转换为领域模型关系
     *
     * @param relation gRPC 关系对象
     * @return 领域模型关系
     */
    private static Relation toDomain(com.company.intelligentdiagnosis.parse.Relation relation) {
        com.company.intelligentdiagnosis.parse.RelationKind protoKind = relation.getKind();
        RelationKind kind = (protoKind == null || protoKind == com.company.intelligentdiagnosis.parse.RelationKind.UNRECOGNIZED)
            ? RelationKind.DEPENDS_ON
            : RelationKind.valueOf(protoKind.name());
        return new Relation(kind, relation.getTargetId());
    }
}
