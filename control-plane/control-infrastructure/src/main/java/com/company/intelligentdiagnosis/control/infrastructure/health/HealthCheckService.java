package com.company.intelligentdiagnosis.control.infrastructure.health;

import org.springframework.stereotype.Service;

import java.net.Socket;
import java.time.Duration;
import java.util.List;

@Service
public class HealthCheckService {

    private static final int TIMEOUT_MS = 3000;

    public InfrastructureHealth checkPostgreSQL(String host, int port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            return new InfrastructureHealth(true, (int) latency, "PostgreSQL", "元数据存储", host + ":" + port);
        } catch (Exception e) {
            return new InfrastructureHealth(false, 0, "PostgreSQL", "元数据存储", host + ":" + port);
        }
    }

    public InfrastructureHealth checkQdrant(String host, int port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            return new InfrastructureHealth(true, (int) latency, "Qdrant", "向量数据库", host + ":" + port);
        } catch (Exception e) {
            return new InfrastructureHealth(false, 0, "Qdrant", "向量数据库", host + ":" + port);
        }
    }

    public InfrastructureHealth checkNeo4j(String host, int port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            return new InfrastructureHealth(true, (int) latency, "Neo4j", "图数据库", host + ":" + port);
        } catch (Exception e) {
            return new InfrastructureHealth(false, 0, "Neo4j", "图数据库", host + ":" + port);
        }
    }

    public InfrastructureHealth checkTemporal(String host, int port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            return new InfrastructureHealth(true, (int) latency, "Temporal", "工作流引擎", host + ":" + port);
        } catch (Exception e) {
            return new InfrastructureHealth(false, 0, "Temporal", "工作流引擎", host + ":" + port);
        }
    }

    public InfrastructureHealth checkRedis(String host, int port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            return new InfrastructureHealth(true, (int) latency, "Redis", "缓存", host + ":" + port);
        } catch (Exception e) {
            return new InfrastructureHealth(false, 0, "Redis", "缓存", host + ":" + port);
        }
    }

    public List<InfrastructureHealth> checkAll() {
        return List.of(
            checkPostgreSQL("localhost", 5432),
            checkQdrant("localhost", 6333),
            checkNeo4j("localhost", 7687),
            checkTemporal("localhost", 7233),
            checkRedis("localhost", 6379)
        );
    }

    public record InfrastructureHealth(
        boolean connected,
        int latency,
        String name,
        String type,
        String url
    ) {}
}
