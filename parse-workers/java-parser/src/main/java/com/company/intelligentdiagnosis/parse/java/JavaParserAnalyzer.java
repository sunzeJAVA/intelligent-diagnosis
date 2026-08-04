package com.company.intelligentdiagnosis.parse.java;

import com.company.intelligentdiagnosis.parse.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Java 代码分析器
 * <p>
 * 使用 JavaParser + SymbolSolver 解析 Java 源代码，提取代码元素和关系：
 * <ul>
 *   <li>{@link RelationKind#CONTAINS} - 类包含方法</li>
 *   <li>{@link RelationKind#EXTENDS} - 类继承父类</li>
 *   <li>{@link RelationKind#IMPLEMENTS} - 类实现接口</li>
 *   <li>{@link RelationKind#DEPENDS_ON} - 方法依赖类型（参数、返回值、字段类型）</li>
 *   <li>{@link RelationKind#CALLS} - 方法调用关系（需符号解析）</li>
 * </ul>
 * <p>
 * SymbolSolver 配置：CombinedTypeSolver 组合三种类型解析器：
 * <ul>
 *   <li>{@link ReflectionTypeSolver} - 解析 JDK 标准库类型</li>
 *   <li>{@link JavaParserTypeSolver} - 解析项目内部跨文件类型</li>
 * </ul>
 * <p>
 * 外部类型处理：JDK 等外部类型不创建节点，对应的关系会被跳过（避免静默丢弃）。
 */
@Component
public class JavaParserAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(JavaParserAnalyzer.class);

    private final JavaParser javaParser;

    public JavaParserAnalyzer() {
        this.javaParser = new JavaParser();
    }

    /**
     * 分析指定仓库的 Java 文件
     *
     * @param repoPath 仓库根路径
     * @param files    相对路径文件列表
     * @return 解析出的代码元素列表
     */
    public List<CodeElement> analyze(Path repoPath, List<String> files) {
        // 每次分析重建 SymbolSolver，包含仓库源码路径，支持跨文件符号解析
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(repoPath));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        javaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        List<CodeElement> elements = new ArrayList<>();
        // 收集所有已解析类型的全限定名，用于判断目标是否为项目内部类型
        Set<String> internalQualifiedNames = new HashSet<>();

        // 第一遍：解析所有文件，提取元素并收集内部类型清单
        List<CompilationUnit> compilationUnits = new ArrayList<>();
        List<String> filePaths = new ArrayList<>();
        for (String file : files) {
            if (!file.endsWith(".java")) {
                continue;
            }
            Path filePath = repoPath.resolve(file);
            if (!Files.exists(filePath)) {
                continue;
            }
            try {
                ParseResult<CompilationUnit> result = javaParser.parse(filePath);
                result.getResult().ifPresent(cu -> {
                    compilationUnits.add(cu);
                    filePaths.add(file);
                    extractElements(cu, file, elements, internalQualifiedNames);
                });
            } catch (IOException e) {
                log.warn("Failed to parse {}: {}", file, e.getMessage());
            }
        }

        log.info("First pass: extracted {} elements, {} internal types from {} files",
            elements.size(), internalQualifiedNames.size(), compilationUnits.size());

        // 第二遍：补充跨文件关系（CALLS、DEPENDS_ON、IMPLEMENTS、EXTENDS）
        for (int i = 0; i < compilationUnits.size(); i++) {
            extractCrossFileRelations(compilationUnits.get(i), filePaths.get(i), elements, internalQualifiedNames);
        }

        log.info("Second pass: enriched relations, total {} elements", elements.size());
        return elements;
    }

    /**
     * 第一遍：提取元素和 CONTAINS 关系，收集内部类型清单
     */
    private void extractElements(CompilationUnit cu, String filePath,
                                  List<CodeElement> elements,
                                  Set<String> internalQualifiedNames) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(type -> {
            String qualifiedName = type.getFullyQualifiedName().orElse(type.getNameAsString());
            internalQualifiedNames.add(qualifiedName);

            String classId = filePath + "#" + qualifiedName;
            CodeElement classElement = CodeElement.newBuilder()
                .setId(classId)
                .setKind(type.isInterface() ? ElementKind.INTERFACE : ElementKind.CLASS)
                .setName(type.getNameAsString())
                .setQualifiedName(qualifiedName)
                .setFilePath(filePath)
                .setStartLine(type.getBegin().map(p -> p.line).orElse(0))
                .setEndLine(type.getEnd().map(p -> p.line).orElse(0))
                .build();

            elements.add(classElement);

            type.findAll(MethodDeclaration.class).forEach(method -> {
                String methodId = classId + "." + method.getDeclarationAsString();
                CodeElement methodElement = CodeElement.newBuilder()
                    .setId(methodId)
                    .setKind(ElementKind.METHOD)
                    .setName(method.getNameAsString())
                    .setQualifiedName(qualifiedName + "." + method.getDeclarationAsString())
                    .setFilePath(filePath)
                    .setStartLine(method.getBegin().map(p -> p.line).orElse(0))
                    .setEndLine(method.getEnd().map(p -> p.line).orElse(0))
                    .addRelations(Relation.newBuilder()
                        .setKind(RelationKind.CONTAINS)
                        .setTargetId(classId)
                        .build())
                    .build();

                elements.add(methodElement);
            });
        });
    }

    /**
     * 第二遍：补充跨文件关系
     */
    private void extractCrossFileRelations(CompilationUnit cu, String filePath,
                                            List<CodeElement> elements,
                                            Set<String> internalQualifiedNames) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(type -> {
            String qualifiedName = type.getFullyQualifiedName().orElse(type.getNameAsString());
            String classId = filePath + "#" + qualifiedName;

            // EXTENDS：继承父类
            type.getExtendedTypes().forEach(parent -> {
                resolveType(parent).ifPresent(resolvedQName -> {
                    if (internalQualifiedNames.contains(resolvedQName)) {
                        String targetId = findElementId(elements, resolvedQName);
                        if (targetId != null) {
                            addRelationToElement(elements, classId, RelationKind.EXTENDS, targetId);
                        }
                    }
                });
            });

            // IMPLEMENTS：实现接口
            type.getImplementedTypes().forEach(iface -> {
                resolveType(iface).ifPresent(resolvedQName -> {
                    if (internalQualifiedNames.contains(resolvedQName)) {
                        String targetId = findElementId(elements, resolvedQName);
                        if (targetId != null) {
                            addRelationToElement(elements, classId, RelationKind.IMPLEMENTS, targetId);
                        }
                    }
                });
            });

            // DEPENDS_ON：字段类型依赖
            type.findAll(FieldDeclaration.class).forEach(field -> {
                field.getVariables().forEach(var -> {
                    resolveType(var.getType()).ifPresent(resolvedQName -> {
                        if (internalQualifiedNames.contains(resolvedQName)) {
                            String targetId = findElementId(elements, resolvedQName);
                            if (targetId != null) {
                                addRelationToElement(elements, classId, RelationKind.DEPENDS_ON, targetId);
                            }
                        }
                    });
                });
            });

            // CALLS + DEPENDS_ON：方法级别
            type.findAll(MethodDeclaration.class).forEach(method -> {
                String methodId = classId + "." + method.getDeclarationAsString();

                // DEPENDS_ON：方法参数和返回值类型
                method.getParameters().forEach(param -> {
                    resolveType(param.getType()).ifPresent(resolvedQName -> {
                        if (internalQualifiedNames.contains(resolvedQName)) {
                            String targetId = findElementId(elements, resolvedQName);
                            if (targetId != null) {
                                addRelationToElement(elements, methodId, RelationKind.DEPENDS_ON, targetId);
                            }
                        }
                    });
                });

                // DEPENDS_ON：方法返回值类型
                Type returnType = method.getType();
                if (returnType.isClassOrInterfaceType()) {
                    resolveType(returnType).ifPresent(resolvedQName -> {
                        if (internalQualifiedNames.contains(resolvedQName)) {
                            String targetId = findElementId(elements, resolvedQName);
                            if (targetId != null) {
                                addRelationToElement(elements, methodId, RelationKind.DEPENDS_ON, targetId);
                            }
                        }
                    });
                }

                // CALLS：方法调用（需符号解析）
                method.findAll(MethodCallExpr.class).forEach(call -> {
                    try {
                        ResolvedMethodDeclaration resolved = call.resolve();
                        String declaringQName = resolved.declaringType().getQualifiedName();
                        if (internalQualifiedNames.contains(declaringQName)) {
                            String targetId = findMethodElementId(elements, declaringQName,
                                resolved.getName(), resolved.getSignature());
                            if (targetId != null) {
                                addRelationToElement(elements, methodId, RelationKind.CALLS, targetId);
                            }
                        }
                    } catch (Exception e) {
                        // 符号解析失败（外部库、lambda 等）静默跳过
                        log.debug("Failed to resolve method call {}: {}", call.getNameAsString(), e.getMessage());
                    }
                });
            });
        });
    }

    /**
     * 解析类型的全限定名
     */
    private Optional<String> resolveType(Type type) {
        try {
            if (type.isClassOrInterfaceType()) {
                // ClassOrInterfaceType.resolve() 返回 ResolvedType
                ResolvedType resolved = type.resolve();
                if (resolved.isReferenceType()) {
                    ResolvedReferenceType refType = resolved.asReferenceType();
                    return Optional.of(refType.getQualifiedName());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve type {}: {}", type, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * 根据全限定名查找元素 ID（类/接口）
     */
    private String findElementId(List<CodeElement> elements, String qualifiedName) {
        for (CodeElement e : elements) {
            if (qualifiedName.equals(e.getQualifiedName())) {
                return e.getId();
            }
        }
        return null;
    }

    /**
     * 根据声明类全限定名 + 方法名查找方法元素 ID
     * 方法 qualifiedName 格式：className.methodDeclarationAsString（含修饰符、返回类型、参数）
     * 如：com.example.BaseService.protected void log(String message)
     */
    private String findMethodElementId(List<CodeElement> elements, String declaringQName,
                                        String methodName, String signature) {
        for (CodeElement e : elements) {
            if (e.getKind() != ElementKind.METHOD) continue;
            if (!e.getQualifiedName().startsWith(declaringQName + ".")) continue;
            // 方法名直接匹配（方法 qualifiedName 含修饰符等，但 name 字段是纯方法名）
            if (e.getName().equals(methodName)) {
                return e.getId();
            }
        }
        return null;
    }

    /**
     * 给指定元素添加关系（如果不存在重复）
     */
    private void addRelationToElement(List<CodeElement> elements, String elementId,
                                       RelationKind kind, String targetId) {
        for (int i = 0; i < elements.size(); i++) {
            CodeElement e = elements.get(i);
            if (e.getId().equals(elementId)) {
                List<Relation> relations = new ArrayList<>(e.getRelationsList());
                // 避免重复关系
                boolean exists = relations.stream()
                    .anyMatch(r -> r.getKind() == kind && r.getTargetId().equals(targetId));
                if (!exists) {
                    relations.add(Relation.newBuilder()
                        .setKind(kind)
                        .setTargetId(targetId)
                        .build());
                    elements.set(i, e.toBuilder().clearRelations().addAllRelations(relations).build());
                }
                break;
            }
        }
    }
}
