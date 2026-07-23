package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeRetriever;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeSnippet;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.infrastructure.vector.VectorStoreClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QdrantCodeRetriever implements CodeRetriever {

    private static final Logger log = LoggerFactory.getLogger(QdrantCodeRetriever.class);

    private final VectorStoreClient vectorStoreClient;
    private final DiagnosisProperties diagnosisProperties;

    public QdrantCodeRetriever(VectorStoreClient vectorStoreClient, DiagnosisProperties diagnosisProperties) {
        this.vectorStoreClient = vectorStoreClient;
        this.diagnosisProperties = diagnosisProperties;
    }

    @Override
    public List<CodeSnippet> retrieve(DiagnosisRequest request) {
        String queryText = buildQueryText(request);
        log.info("Retrieving code snippets for service {} with query: {}", request.service(), queryText);

        List<Points.ScoredPoint> points = vectorStoreClient.search(
            request.service(),
            queryText,
            diagnosisProperties.getTopK()
        );

        return points.stream()
            .map(this::toCodeSnippet)
            .toList();
    }

    private String buildQueryText(DiagnosisRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request.query() != null && !request.query().isBlank()) {
            builder.append(request.query()).append(" ");
        }
        if (request.errorInfo() != null && !request.errorInfo().isBlank()) {
            builder.append(request.errorInfo());
        }
        return builder.toString().trim();
    }

    private CodeSnippet toCodeSnippet(Points.ScoredPoint point) {
        var payload = point.getPayloadMap();
        String filePath = stringValue(payload.get("filePath"));
        int startLine = intValue(payload.get("startLine"));
        int endLine = intValue(payload.get("endLine"));
        String content = stringValue(payload.get("sourceCode"));
        if (content.isBlank()) {
            content = stringValue(payload.get("text"));
        }
        return new CodeSnippet(filePath, startLine, endLine, content);
    }

    private String stringValue(JsonWithInt.Value value) {
        if (value == null) {
            return "";
        }
        return value.getStringValue();
    }

    private int intValue(JsonWithInt.Value value) {
        if (value == null) {
            return 0;
        }
        return (int) value.getIntegerValue();
    }
}
