package com.company.intelligentdiagnosis.agent.infrastructure.parse;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.ElementKind;
import com.company.intelligentdiagnosis.agent.domain.RelationKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodeElementMapperTest {

    @Test
    void shouldMapProtoCodeElementToDomain() {
        var protoElement = com.company.intelligentdiagnosis.parse.CodeElement.newBuilder()
            .setId("repo/src/Main.java#Main")
            .setKind(com.company.intelligentdiagnosis.parse.ElementKind.CLASS)
            .setName("Main")
            .setQualifiedName("com.example.Main")
            .setFilePath("src/Main.java")
            .setStartLine(1)
            .setEndLine(10)
            .setSourceCode("class Main {}")
            .setDocumentation("Main class")
            .addModifiers("public")
            .addRelations(com.company.intelligentdiagnosis.parse.Relation.newBuilder()
                .setKind(com.company.intelligentdiagnosis.parse.RelationKind.CONTAINS)
                .setTargetId("repo/src/Main.java#Main#main")
                .build())
            .putMetadata("language", "java")
            .build();

        CodeElement domain = CodeElementMapper.toDomain(protoElement);

        assertThat(domain.id()).isEqualTo("repo/src/Main.java#Main");
        assertThat(domain.kind()).isEqualTo(ElementKind.CLASS);
        assertThat(domain.name()).isEqualTo("Main");
        assertThat(domain.qualifiedName()).isEqualTo("com.example.Main");
        assertThat(domain.filePath()).isEqualTo("src/Main.java");
        assertThat(domain.startLine()).isEqualTo(1);
        assertThat(domain.endLine()).isEqualTo(10);
        assertThat(domain.sourceCode()).isEqualTo("class Main {}");
        assertThat(domain.documentation()).isEqualTo("Main class");
        assertThat(domain.modifiers()).containsExactly("public");
        assertThat(domain.metadata()).containsEntry("language", "java");
        assertThat(domain.relations()).hasSize(1);
        assertThat(domain.relations().get(0).kind()).isEqualTo(RelationKind.CONTAINS);
        assertThat(domain.relations().get(0).targetId()).isEqualTo("repo/src/Main.java#Main#main");
    }

    @Test
    void shouldMapEmptyListsAndMetadata() {
        var protoElement = com.company.intelligentdiagnosis.parse.CodeElement.newBuilder()
            .setId("y")
            .setKind(com.company.intelligentdiagnosis.parse.ElementKind.METHOD)
            .setName("y")
            .build();

        CodeElement domain = CodeElementMapper.toDomain(protoElement);

        assertThat(domain.modifiers()).isEmpty();
        assertThat(domain.relations()).isEmpty();
        assertThat(domain.metadata()).isEmpty();
    }
}
