package com.company.intelligentdiagnosis.agent.infrastructure.parse;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.parse.ParseRequest;
import com.company.intelligentdiagnosis.parse.ParseWorkerGrpc;
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

@Component
public class ParseWorkerClient {

    private static final Logger log = LoggerFactory.getLogger(ParseWorkerClient.class);

    private final ParseWorkerProperties properties;
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    public ParseWorkerClient(ParseWorkerProperties properties) {
        this.properties = properties;
    }

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
            throw new RuntimeException("Parse worker call failed for language " + language, e);
        }
    }

    private ManagedChannel createChannel(ParseWorkerProperties.Endpoint endpoint) {
        log.info("Creating gRPC channel to parse worker {}:{}", endpoint.host(), endpoint.port());
        return ManagedChannelBuilder.forAddress(endpoint.host(), endpoint.port())
            .usePlaintext()
            .maxInboundMessageSize(100 * 1024 * 1024)
            .build();
    }

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
