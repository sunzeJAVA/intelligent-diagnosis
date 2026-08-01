package com.company.intelligentdiagnosis.agent.infrastructure.parse;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.parse.ParseRequest;
import com.company.intelligentdiagnosis.parse.ParseWorkerGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 解析工作者客户端
 * 通过 gRPC 调用解析工作者服务，解析代码文件并获取代码元素
 */
@Component
public class ParseWorkerClient {

    private static final Logger log = LoggerFactory.getLogger(ParseWorkerClient.class);

    /**
     * 解析工作者配置属性
     */
    private final ParseWorkerProperties properties;

    /**
     * 语言到 gRPC 通道的映射，复用通道提高性能
     */
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    /**
     * 创建实例
     *
     * @param properties 解析工作者配置属性
     */
    public ParseWorkerClient(ParseWorkerProperties properties) {
        this.properties = properties;
    }

    /**
     * 调用解析工作者解析代码
     *
     * @param language 编程语言
     * @param request  解析请求
     * @return 代码元素列表
     */
    @CircuitBreaker(name = "parse-worker")
    public List<CodeElement> parse(String language, ParseRequest request) {
        ParseWorkerProperties.Endpoint endpoint = properties.endpointFor(language);
        if (endpoint == null) {
            throw new IllegalArgumentException("No parse worker configured for language: " + language);
        }

        ManagedChannel channel = channels.computeIfAbsent(language, k -> createChannel(endpoint));
        ParseWorkerGrpc.ParseWorkerBlockingStub stub = ParseWorkerGrpc.newBlockingStub(channel)
            .withDeadlineAfter(120, TimeUnit.SECONDS);

        try {
            com.company.intelligentdiagnosis.parse.ParseResponse response = stub.parse(request);
            return response.getElementsList().stream()
                .map(CodeElementMapper::toDomain)
                .toList();
        } catch (Exception e) {
            log.error("Parse worker call failed for language {} at {}:{}", language, endpoint.host(), endpoint.port(), e);
            throw new ParseWorkerUnavailableException("Parse worker call failed for language " + language, e);
        }
    }

    /**
     * 创建 gRPC 通道
     *
     * @param endpoint 端点配置
     * @return gRPC 通道
     */
    private ManagedChannel createChannel(ParseWorkerProperties.Endpoint endpoint) {
        log.info("Creating gRPC channel to parse worker {}:{}", endpoint.host(), endpoint.port());
        return ManagedChannelBuilder.forAddress(endpoint.host(), endpoint.port())
            .usePlaintext()
            .maxInboundMessageSize(100 * 1024 * 1024)
            .build();
    }

    /**
     * 关闭所有 gRPC 通道
     */
    @PreDestroy
    public void shutdown() {
        channels.values().forEach(channel -> {
            try {
                channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
