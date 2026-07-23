package com.company.intelligentdiagnosis.agent.infrastructure.vector;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "embedding.provider", havingValue = "token-hash", matchIfMissing = true)
public class TokenHashEmbeddingGenerator implements EmbeddingGenerator {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final int dimension;

    public TokenHashEmbeddingGenerator() {
        this(384);
    }

    public TokenHashEmbeddingGenerator(int dimension) {
        this.dimension = dimension;
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[dimension];
        }

        float[] vector = new float[dimension];
        var matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));

        while (matcher.find()) {
            String token = matcher.group();
            int bucket = bucketFor(token);
            float weight = 1.0f + (float) Math.log1p(token.length());
            vector[bucket] += weight;
        }

        return normalize(vector);
    }

    @Override
    public int dimension() {
        return dimension;
    }

    private int bucketFor(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            int value = ((hash[0] & 0xFF) << 24)
                | ((hash[1] & 0xFF) << 16)
                | ((hash[2] & 0xFF) << 8)
                | (hash[3] & 0xFF);
            return Math.abs(value) % dimension;
        } catch (NoSuchAlgorithmException e) {
            return Math.abs(token.hashCode()) % dimension;
        }
    }

    private float[] normalize(float[] vector) {
        float sumSquares = 0.0f;
        for (float value : vector) {
            sumSquares += value * value;
        }
        if (sumSquares == 0.0f) {
            return vector;
        }
        float magnitude = (float) Math.sqrt(sumSquares);
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / magnitude;
        }
        return normalized;
    }
}
