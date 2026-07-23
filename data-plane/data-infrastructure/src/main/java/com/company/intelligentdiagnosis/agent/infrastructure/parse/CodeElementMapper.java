package com.company.intelligentdiagnosis.agent.infrastructure.parse;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.ElementKind;
import com.company.intelligentdiagnosis.agent.domain.Relation;
import com.company.intelligentdiagnosis.agent.domain.RelationKind;

import java.util.List;
import java.util.Map;

public final class CodeElementMapper {

    private CodeElementMapper() {
    }

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

    private static Relation toDomain(com.company.intelligentdiagnosis.parse.Relation relation) {
        com.company.intelligentdiagnosis.parse.RelationKind protoKind = relation.getKind();
        RelationKind kind = (protoKind == null || protoKind == com.company.intelligentdiagnosis.parse.RelationKind.UNRECOGNIZED)
            ? RelationKind.DEPENDS_ON
            : RelationKind.valueOf(protoKind.name());
        return new Relation(kind, relation.getTargetId());
    }
}
