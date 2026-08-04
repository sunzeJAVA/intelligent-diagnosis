package com.company.intelligentdiagnosis.control.infrastructure.health;

import org.springframework.stereotype.Service;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parse Worker 健康检查器
 * <p>
 * 通过 TCP Socket 探测各语言解析 Worker 的 gRPC 端口可达性。
 */
@Service
public class ParseWorkerHealthChecker {

    private static final int TIMEOUT_MS = 3000;

    private final ParseWorkerProperties properties;

    public ParseWorkerHealthChecker(ParseWorkerProperties properties) {
        this.properties = properties;
    }

    public List<ParseWorkerHealth> checkAll() {
        List<ParseWorkerHealth> result = new ArrayList<>();
        Map<String, ParseWorkerProperties.Endpoint> endpoints = properties.getEndpoints();
        if (endpoints == null || endpoints.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, ParseWorkerProperties.Endpoint> entry : endpoints.entrySet()) {
            String name = entry.getKey();
            ParseWorkerProperties.Endpoint ep = entry.getValue();
            long start = System.currentTimeMillis();
            boolean healthy;
            int latency;
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(ep.host(), ep.port()), TIMEOUT_MS);
                healthy = true;
                latency = (int) (System.currentTimeMillis() - start);
            } catch (Exception e) {
                healthy = false;
                latency = 0;
            }
            result.add(new ParseWorkerHealth(
                name + "-parser",
                ep.language() != null ? ep.language() : name,
                ep.host() + ":" + ep.port(),
                healthy,
                latency
            ));
        }
        return result;
    }

    public record ParseWorkerHealth(
        String name,
        String language,
        String address,
        boolean healthy,
        int latency
    ) {}
}
