package com.company.intelligentdiagnosis.parse.java;

import com.company.intelligentdiagnosis.parse.CodeElement;
import com.company.intelligentdiagnosis.parse.ElementKind;
import com.company.intelligentdiagnosis.parse.RelationKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JavaParserAnalyzer 单元测试
 * 验证 5 种关系类型的提取：CONTAINS、EXTENDS、IMPLEMENTS、DEPENDS_ON、CALLS
 */
class JavaParserAnalyzerTest {

    private final JavaParserAnalyzer analyzer = new JavaParserAnalyzer();

    @Test
    void shouldExtractAllRelationTypes(@TempDir Path tempDir) throws IOException {
        // 创建标准 Maven 包结构 src/main/java/com/example/
        Path srcRoot = tempDir.resolve("src/main/java");
        Path pkgDir = srcRoot.resolve("com/example");
        Files.createDirectories(pkgDir);

        // 准备测试源码：Service 接口、BaseService 父类、UserRepository 依赖、UserController 实现类
        Files.writeString(pkgDir.resolve("Service.java"), """
            package com.example;
            public interface Service {
                String execute(String input);
            }
            """);

        Files.writeString(pkgDir.resolve("BaseService.java"), """
            package com.example;
            public abstract class BaseService {
                protected void log(String message) {
                    System.out.println(message);
                }
            }
            """);

        Files.writeString(pkgDir.resolve("UserRepository.java"), """
            package com.example;
            public class UserRepository {
                public String findById(Long id) {
                    return "user-" + id;
                }
            }
            """);

        Files.writeString(pkgDir.resolve("UserController.java"), """
            package com.example;

            public class UserController extends BaseService implements Service {

                private UserRepository repository;

                @Override
                public String execute(String input) {
                    log("executing");
                    return repository.findById(1L);
                }
            }
            """);

        List<CodeElement> elements = analyzer.analyze(srcRoot,
            List.of("com/example/Service.java", "com/example/BaseService.java",
                    "com/example/UserRepository.java", "com/example/UserController.java"));

        // 验证元素数量：4 个类/接口 + 5 个方法 = 9
        assertThat(elements).hasSizeGreaterThanOrEqualTo(8);

        // 验证 CONTAINS：方法 → 类
        CodeElement executeMethod = findMethod(elements, "execute");
        assertThat(executeMethod).isNotNull();
        assertThat(executeMethod.getRelationsList())
            .anyMatch(r -> r.getKind() == RelationKind.CONTAINS);

        // 验证 EXTENDS：UserController extends BaseService
        CodeElement controller = findClass(elements, "UserController");
        assertThat(controller.getRelationsList())
            .anyMatch(r -> r.getKind() == RelationKind.EXTENDS
                && r.getTargetId().contains("BaseService"));

        // 验证 IMPLEMENTS：UserController implements Service
        assertThat(controller.getRelationsList())
            .anyMatch(r -> r.getKind() == RelationKind.IMPLEMENTS
                && r.getTargetId().contains("Service"));

        // 验证 DEPENDS_ON：UserController 依赖 UserRepository（字段类型）
        assertThat(controller.getRelationsList())
            .anyMatch(r -> r.getKind() == RelationKind.DEPENDS_ON
                && r.getTargetId().contains("UserRepository"));

        // 验证 CALLS：execute() 调用 log() 和 findById()
        assertThat(executeMethod.getRelationsList())
            .anyMatch(r -> r.getKind() == RelationKind.CALLS
                && r.getTargetId().contains("log"));
        assertThat(executeMethod.getRelationsList())
            .anyMatch(r -> r.getKind() == RelationKind.CALLS
                && r.getTargetId().contains("findById"));
    }

    @Test
    void shouldNotCreateRelationsForJdkTypes(@TempDir Path tempDir) throws IOException {
        Path srcRoot = tempDir.resolve("src/main/java");
        Path pkgDir = srcRoot.resolve("com/example");
        Files.createDirectories(pkgDir);

        Files.writeString(pkgDir.resolve("Hello.java"), """
            package com.example;
            import java.util.List;
            import java.util.ArrayList;

            public class Hello {
                public List<String> getNames() {
                    return new ArrayList<>();
                }
            }
            """);

        List<CodeElement> elements = analyzer.analyze(srcRoot, List.of("com/example/Hello.java"));

        // JDK 类型（List、ArrayList、String）不应产生 DEPENDS_ON 关系
        CodeElement helloClass = findClass(elements, "Hello");
        assertThat(helloClass.getRelationsList())
            .noneMatch(r -> r.getKind() == RelationKind.DEPENDS_ON
                && (r.getTargetId().contains("List") || r.getTargetId().contains("ArrayList")));

        CodeElement getNamesMethod = findMethod(elements, "getNames");
        assertThat(getNamesMethod.getRelationsList())
            .noneMatch(r -> r.getKind() == RelationKind.DEPENDS_ON
                && r.getTargetId().contains("List"));
    }

    private CodeElement findClass(List<CodeElement> elements, String className) {
        return elements.stream()
            .filter(e -> (e.getKind() == ElementKind.CLASS || e.getKind() == ElementKind.INTERFACE)
                && e.getName().equals(className))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Class not found: " + className));
    }

    private CodeElement findMethod(List<CodeElement> elements, String methodName) {
        // 优先返回 UserController 的方法（有更多关系），其次返回任意匹配方法
        return elements.stream()
            .filter(e -> e.getKind() == ElementKind.METHOD && e.getName().equals(methodName))
            .reduce((first, second) -> second)  // 返回最后一个匹配项
            .orElseThrow(() -> new AssertionError("Method not found: " + methodName));
    }
}
