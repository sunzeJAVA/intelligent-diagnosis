package com.company.intelligentdiagnosis.control.infrastructure.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * 基础设施健康检查服务
 * <p>
 * 通过 TCP Socket 探测端口可达性并测量延迟；对支持 HTTP/JDBC 的服务额外探测真实版本号。
 * 版本探测失败不影响连接状态，版本字段返回 "unknown"。
 */
@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);
    private static final int TIMEOUT_MS = 3000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final InfrastructureProperties properties;
    private final DataSource dataSource;
    private final RestClient restClient;

    public HealthCheckService(InfrastructureProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.restClient = RestClient.builder()
            .build();
    }

    public InfrastructureHealth checkPostgreSQL() {
        InfrastructureProperties.Endpoint ep = properties.getPostgresql();
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(ep.host(), ep.port()), TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            return new InfrastructureHealth(true, (int) latency, "PostgreSQL", "元数据存储",
                ep.host() + ":" + ep.port(), probePostgreSQLVersion());
        } catch (Exception e) {
            return new InfrastructureHealth(false, 0, "PostgreSQL", "元数据存储",
                ep.host() + ":" + ep.port(), "unknown");
        }
    }

    public InfrastructureHealth checkQdrant() {
        InfrastructureProperties.Endpoint ep = properties.getQdrant();
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(ep.host(), ep.port()), TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            return new InfrastructureHealth(true, (int) latency, "Qdrant", "向量数据库",
                ep.host() + ":" + ep.port(), probeQdrantVersion(ep.host(), ep.port()));
        } catch (Exception e) {
            return new InfrastructureHealth(false, 0, "Qdrant", "向量数据库",
                ep.host() + ":" + ep.port(), "unknown");
        }
    }

    public InfrastructureHealth checkNeo4j() {
        InfrastructureProperties.Endpoint ep = properties.getNeo4j();
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(ep.host(), ep.port()), TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            return new InfrastructureHealth(true, (int) latency, "Neo4j", "图数据库",
                ep.host() + ":" + ep.port(), probeNeo4jVersion(ep.host(), properties.getNeo4jHttpPort()));
        } catch (Exception e) {
            return new InfrastructureHealth(false, 0, "Neo4j", "图数据库",
                ep.host() + ":" + ep.port(), "unknown");
        }
    }

    public InfrastructureHealth checkTemporal() {
        InfrastructureProperties.Endpoint ep = properties.getTemporal();
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(ep.host(), ep.port()), TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            return new InfrastructureHealth(true, (int) latency, "Temporal", "工作流引擎",
                ep.host() + ":" + ep.port(), "unknown");
        } catch (Exception e) {
            return new InfrastructureHealth(false, 0, "Temporal", "工作流引擎",
                ep.host() + ":" + ep.port(), "unknown");
        }
    }

    public InfrastructureHealth checkRedis() {
        InfrastructureProperties.Endpoint ep = properties.getRedis();
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(ep.host(), ep.port()), TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            return new InfrastructureHealth(true, (int) latency, "Redis", "缓存",
                ep.host() + ":" + ep.port(), "unknown");
        } catch (Exception e) {
            return new InfrastructureHealth(false, 0, "Redis", "缓存",
                ep.host() + ":" + ep.port(), "unknown");
        }
    }

    public List<InfrastructureHealth> checkAll() {
        return List.of(
            checkPostgreSQL(),
            checkQdrant(),
            checkNeo4j(),
            checkTemporal(),
            checkRedis()
        );
    }

    /**
     * 通过 JDBC 查询 PostgreSQL 真实版本
     */
    private String probePostgreSQLVersion() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version()")) {
            if (rs.next()) {
                // 示例：PostgreSQL 16.2 on aarch64-apple-darwin23.6.0
                String full = rs.getString(1);
                // 提取 "PostgreSQL 16.x.x" 部分
                int firstSpace = full.indexOf(' ');
                int secondSpace = full.indexOf(' ', firstSpace + 1);
                if (firstSpace > 0 && secondSpace > firstSpace) {
                    return full.substring(firstSpace + 1, secondSpace);
                }
                return full;
            }
        } catch (Exception e) {
            log.debug("Failed to probe PostgreSQL version: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * 通过 HTTP GET / 探测 Qdrant 版本
     * 响应示例：{"title":"qdrant","version":"1.10.0",...}
     */
    private String probeQdrantVersion(String host, int port) {
        try {
            String body = restClient.get()
                .uri("http://" + host + ":" + port + "/")
                .retrieve()
                .body(String.class);
            if (body != null) {
                JsonNode root = OBJECT_MAPPER.readTree(body);
                JsonNode version = root.get("version");
                if (version != null) {
                    return version.asText();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to probe Qdrant version: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * 通过 HTTP GET / 探测 Neo4j 版本
     * 响应示例：{"bolt_direct":"bolt://localhost:7687","neo4j_version":"5.20.0",...}
     */
    private String probeNeo4jVersion(String host, int httpPort) {
        try {
            String body = restClient.get()
                .uri("http://" + host + ":" + httpPort + "/")
                .retrieve()
                .body(String.class);
            if (body != null) {
                JsonNode root = OBJECT_MAPPER.readTree(body);
                JsonNode version = root.get("neo4j_version");
                if (version != null) {
                    return version.asText();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to probe Neo4j version: {}", e.getMessage());
        }
        return "unknown";
    }

    public record InfrastructureHealth(
        boolean connected,
        int latency,
        String name,
        String type,
        String url,
        String version
    ) {}
}
