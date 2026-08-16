package com.company.intelligentdiagnosis.parse.java;

import com.company.intelligentdiagnosis.parse.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@GrpcService
public class JavaParseWorkerService extends ParseWorkerGrpc.ParseWorkerImplBase {

    private static final Logger log = LoggerFactory.getLogger(JavaParseWorkerService.class);

    private final JavaParserAnalyzer analyzer;

    public JavaParseWorkerService(JavaParserAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public void parse(ParseRequest request, StreamObserver<ParseResponse> responseObserver) {
        long start = System.currentTimeMillis();
        log.info("Parsing Java files for repository: {}, commit: {}", request.getRepository(), request.getCommitHash());

        try {
            Path repoPath = Paths.get(request.getRepoPath());
            List<String> files = request.getChangedFilesList();

            // changedFiles 为空时，扫描仓库中所有 .java 文件（全量解析）
            if (files.isEmpty()) {
                log.info("No changed files specified, scanning all .java files in {}", repoPath);
                files = scanAllJavaFiles(repoPath);
                log.info("Found {} Java files for full parse", files.size());
            }

            List<CodeElement> elements = analyzer.analyze(repoPath, files);

            ParseResponse response = ParseResponse.newBuilder()
                .addAllElements(elements)
                .setDurationMs(System.currentTimeMillis() - start)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to parse Java files", e);
            ParseError error = ParseError.newBuilder()
                .setMessage(e.getMessage())
                .setSeverity(ErrorSeverity.FATAL)
                .build();

            responseObserver.onNext(ParseResponse.newBuilder()
                .addErrors(error)
                .setDurationMs(System.currentTimeMillis() - start)
                .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 扫描仓库中所有 .java 文件的相对路径
     *
     * @param repoPath 仓库根路径
     * @return 相对于 repoPath 的 .java 文件路径列表
     */
    private List<String> scanAllJavaFiles(Path repoPath) {
        List<String> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(repoPath)) {
            stream.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> files.add(repoPath.relativize(p).toString().replace('\\', '/')));
        } catch (IOException e) {
            log.error("Failed to scan Java files in {}", repoPath, e);
        }
        return files;
    }

    @Override
    public void healthCheck(HealthRequest request, StreamObserver<HealthResponse> responseObserver) {
        responseObserver.onNext(HealthResponse.newBuilder()
            .setHealthy(true)
            .setVersion("0.1.0")
            .build());
        responseObserver.onCompleted();
    }
}
