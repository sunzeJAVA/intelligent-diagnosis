package com.company.intelligentdiagnosis.parse.java;

import com.company.intelligentdiagnosis.parse.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class JavaParserAnalyzer {

    private final JavaParser javaParser;

    public JavaParserAnalyzer() {
        this.javaParser = new JavaParser();
    }

    public List<CodeElement> analyze(Path repoPath, List<String> files) {
        List<CodeElement> elements = new ArrayList<>();

        for (String file : files) {
            if (!file.endsWith(".java")) {
                continue;
            }

            Path filePath = repoPath.resolve(file);
            if (!Files.exists(filePath)) {
                continue;
            }

            try {
                javaParser.parse(filePath)
                    .getResult()
                    .ifPresent(cu -> extractElements(cu, file, elements));
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse " + file, e);
            }
        }

        return elements;
    }

    private void extractElements(CompilationUnit cu, String filePath, List<CodeElement> elements) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(type -> {
            CodeElement classElement = CodeElement.newBuilder()
                .setId(filePath + "#" + type.getFullyQualifiedName().orElse(type.getNameAsString()))
                .setKind(type.isInterface() ? ElementKind.INTERFACE : ElementKind.CLASS)
                .setName(type.getNameAsString())
                .setQualifiedName(type.getFullyQualifiedName().orElse(type.getNameAsString()))
                .setFilePath(filePath)
                .setStartLine(type.getBegin().map(p -> p.line).orElse(0))
                .setEndLine(type.getEnd().map(p -> p.line).orElse(0))
                .build();

            elements.add(classElement);

            type.findAll(MethodDeclaration.class).forEach(method -> {
                CodeElement methodElement = CodeElement.newBuilder()
                    .setId(classElement.getId() + "." + method.getDeclarationAsString())
                    .setKind(ElementKind.METHOD)
                    .setName(method.getNameAsString())
                    .setQualifiedName(classElement.getQualifiedName() + "." + method.getDeclarationAsString())
                    .setFilePath(filePath)
                    .setStartLine(method.getBegin().map(p -> p.line).orElse(0))
                    .setEndLine(method.getEnd().map(p -> p.line).orElse(0))
                    .addRelations(Relation.newBuilder()
                        .setKind(RelationKind.CONTAINS)
                        .setTargetId(classElement.getId())
                        .build())
                    .build();

                elements.add(methodElement);
            });
        });
    }
}
