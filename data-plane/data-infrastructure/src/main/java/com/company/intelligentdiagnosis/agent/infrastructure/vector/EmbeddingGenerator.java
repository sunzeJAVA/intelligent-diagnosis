package com.company.intelligentdiagnosis.agent.infrastructure.vector;

public interface EmbeddingGenerator {

    float[] embed(String text);

    int dimension();
}
