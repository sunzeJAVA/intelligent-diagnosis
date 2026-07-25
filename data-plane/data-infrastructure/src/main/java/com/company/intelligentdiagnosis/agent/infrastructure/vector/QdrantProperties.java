package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Qdrant 向量数据库配置属性
 */
@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {

    /**
     * Qdrant 服务主机地址
     */
    private String host = "localhost";

    /**
     * Qdrant 服务端口
     */
    private int port = 6334;

    /**
     * 集合名称
     */
    private String collectionName = "code-elements";

    /**
     * 如果集合不存在是否自动创建
     */
    private boolean createCollectionIfMissing = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public boolean isCreateCollectionIfMissing() {
        return createCollectionIfMissing;
    }

    public void setCreateCollectionIfMissing(boolean createCollectionIfMissing) {
        this.createCollectionIfMissing = createCollectionIfMissing;
    }
}
