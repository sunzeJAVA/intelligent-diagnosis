package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {

    private String host = "localhost";
    private int port = 6334;
    private String collectionName = "code-elements";
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
