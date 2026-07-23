package com.company.intelligentdiagnosis.agent.domain;

import java.util.List;
import java.util.Map;

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

    public CodeElement withRelations(List<Relation> newRelations) {
        return new CodeElement(
            id, kind, name, qualifiedName, filePath, startLine, endLine,
            sourceCode, documentation, modifiers, newRelations, metadata
        );
    }
}
