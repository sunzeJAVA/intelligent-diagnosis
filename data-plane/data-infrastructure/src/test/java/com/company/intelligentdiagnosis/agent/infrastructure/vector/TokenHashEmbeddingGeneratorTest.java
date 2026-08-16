package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class TokenHashEmbeddingGeneratorTest {

    private final TokenHashEmbeddingGenerator generator = new TokenHashEmbeddingGenerator();

    @ParameterizedTest
    @NullAndEmptySource
    void shouldReturnZeroVectorForBlankInput(String text) {
        float[] vector = generator.embed(text);

        assertThat(vector).hasSize(generator.dimension());
        assertThat(vector).containsOnly(0.0f);
    }

    @Test
    void shouldReturnNormalizedVectorForNonEmptyText() {
        float[] vector = generator.embed("public class HelloWorld");

        assertThat(vector).hasSize(generator.dimension());

        float sumOfSquares = 0.0f;
        for (float value : vector) {
            sumOfSquares += value * value;
        }
        assertThat(sumOfSquares).isCloseTo(1.0f, offset(0.0001f));
    }

    @Test
    void shouldBeDeterministic() {
        String text = "class Example { void run() {} }";
        float[] first = generator.embed(text);
        float[] second = generator.embed(text);

        assertThat(first).containsExactly(second);
    }

    @Test
    void shouldRespectCustomDimension() {
        TokenHashEmbeddingGenerator custom = new TokenHashEmbeddingGenerator(64);
        float[] vector = custom.embed("test");

        assertThat(vector).hasSize(64);
        assertThat(custom.dimension()).isEqualTo(64);
    }

    @Test
    void shouldDefaultTo384Dimension() {
        assertThat(generator.dimension()).isEqualTo(384);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "a", "abc", "main", "NullPointerException", "Integer.MIN_VALUE",
        "veryLongIdentifierNameThatMightHashToMinValue", "x", "_"
    })
    void shouldAlwaysProduceValidBucketIndex(String token) {
        // 不应抛 ArrayIndexOutOfBoundsException；结果向量维度正确
        float[] vector = generator.embed(token);

        assertThat(vector).hasSize(generator.dimension());
        for (float v : vector) {
            assertThat(Float.isNaN(v)).isFalse();
        }
    }
}
