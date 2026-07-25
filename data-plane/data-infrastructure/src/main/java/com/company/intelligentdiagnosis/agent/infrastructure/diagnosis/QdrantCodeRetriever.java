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

/**
 * 基于 Qdrant 的代码检索器
 * 通过向量相似度搜索检索与诊断请求相关的代码片段
 */
@Component
public class QdrantCodeRetriever implements CodeRetriever {

    private static final Logger log = LoggerFactory.getLogger(QdrantCodeRetriever.class);

    private final VectorStoreClient vectorStoreClient;
    private final DiagnosisProperties diagnosisProperties;

    /**
     * 构造函数
     *
     * @param vectorStoreClient   向量存储客户端
     * @param diagnosisProperties 诊断配置属性
     */
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

    /**
     * 构建查询文本
     * 将诊断请求的查询和错误信息组合成搜索文本
     *
     * @param request 诊断请求
     * @return 查询文本
     */
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

    /**
     * 将向量点转换为代码片段
     *
     * @param point 向量点
     * @return 代码片段
     */
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

    /**
     * 获取字符串值
     *
     * @param value JSON 值
     * @return 字符串值
     */
    private String stringValue(JsonWithInt.Value value) {
        if (value == null) {
            return "";
        }
        return value.getStringValue();
    }

    /**
     * 获取整数值
     *
     * @param value JSON 值
     * @return 整数值
     */
    private int intValue(JsonWithInt.Value value) {
        if (value == null) {
            return 0;
        }
        return (int) value.getIntegerValue();
    }
}
