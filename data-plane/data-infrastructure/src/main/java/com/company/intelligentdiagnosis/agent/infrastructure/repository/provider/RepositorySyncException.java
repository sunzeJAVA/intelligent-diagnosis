package com.company.intelligentdiagnosis.agent.infrastructure.repository.provider;

public class RepositorySyncException extends RuntimeException {

    public RepositorySyncException(String message) {
        super(message);
    }

    public RepositorySyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
