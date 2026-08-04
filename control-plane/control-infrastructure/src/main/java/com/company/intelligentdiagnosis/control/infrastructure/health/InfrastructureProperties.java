package com.company.intelligentdiagnosis.control.infrastructure.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 基础设施连接配置
 * <p>
 * 集中管理各基础设施组件的 host/port，供 {@link HealthCheckService} 使用。
 */
@ConfigurationProperties(prefix = "infrastructure")
public class InfrastructureProperties {

    private Endpoint postgresql = new Endpoint("localhost", 5432);
    private Endpoint qdrant = new Endpoint("localhost", 6333);
    private Endpoint neo4j = new Endpoint("localhost", 7687);
    /** Neo4j HTTP 端口（用于版本探测） */
    private int neo4jHttpPort = 7474;
    private Endpoint temporal = new Endpoint("localhost", 7233);
    private Endpoint redis = new Endpoint("localhost", 6379);

    public Endpoint getPostgresql() { return postgresql; }
    public void setPostgresql(Endpoint postgresql) { this.postgresql = postgresql; }

    public Endpoint getQdrant() { return qdrant; }
    public void setQdrant(Endpoint qdrant) { this.qdrant = qdrant; }

    public Endpoint getNeo4j() { return neo4j; }
    public void setNeo4j(Endpoint neo4j) { this.neo4j = neo4j; }

    public int getNeo4jHttpPort() { return neo4jHttpPort; }
    public void setNeo4jHttpPort(int neo4jHttpPort) { this.neo4jHttpPort = neo4jHttpPort; }

    public Endpoint getTemporal() { return temporal; }
    public void setTemporal(Endpoint temporal) { this.temporal = temporal; }

    public Endpoint getRedis() { return redis; }
    public void setRedis(Endpoint redis) { this.redis = redis; }

    public record Endpoint(String host, int port) {}
}
