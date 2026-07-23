package com.company.intelligentdiagnosis.parse.java;

import com.company.intelligentdiagnosis.parse.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

    @Override
    public void healthCheck(HealthRequest request, StreamObserver<HealthResponse> responseObserver) {
        responseObserver.onNext(HealthResponse.newBuilder()
            .setHealthy(true)
            .setVersion("0.1.0")
            .build());
        responseObserver.onCompleted();
    }
}
