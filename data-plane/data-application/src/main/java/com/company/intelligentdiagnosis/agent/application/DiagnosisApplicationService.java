package com.company.intelligentdiagnosis.agent.application;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeRetriever;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.CodeSnippet;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisAuditor;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisRequest;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisResponse;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.PolicyEngine;
import com.company.intelligentdiagnosis.agent.domain.llm.LlmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiagnosisApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisApplicationService.class);

    private static final String SYSTEM_PROMPT = """
        You are an expert software engineering assistant. Given a user query, error information, service name, and relevant code snippets, analyze the problem and produce a structured diagnosis in Chinese.
        Return ONLY a JSON object with this shape:
        {
          "summary": "one-sentence summary of the issue",
          "rootCause": "detailed root cause analysis",
          "suggestions": ["actionable fix 1", "actionable fix 2", ...]
        }
        Do not include markdown fences, explanations, or any text outside the JSON.
        """;

    private final CodeRetriever codeRetriever;
    private final LlmClient llmClient;
    private final DiagnosisAuditor auditor;
    private final PolicyEngine policyEngine;
    private final ObjectMapper objectMapper;

    public DiagnosisApplicationService(CodeRetriever codeRetriever,
                                       LlmClient llmClient,
                                       DiagnosisAuditor auditor,
                                       PolicyEngine policyEngine,
                                       ObjectMapper objectMapper) {
        this.codeRetriever = codeRetriever;
        this.llmClient = llmClient;
        this.auditor = auditor;
        this.policyEngine = policyEngine;
        this.objectMapper = objectMapper;
    }

    public DiagnosisResponse diagnose(DiagnosisRequest request) {
        long start = System.currentTimeMillis();
        policyEngine.validate(request);

        List<CodeSnippet> relatedCode = codeRetriever.retrieve(request);
        String userPrompt = buildUserPrompt(request, relatedCode);

        String rawResponse = llmClient.complete(SYSTEM_PROMPT, userPrompt);
        DiagnosisResponse response = parseResponse(rawResponse, relatedCode);

        long duration = System.currentTimeMillis() - start;
        auditor.record(request, response, duration);

        return response;
    }

    private String buildUserPrompt(DiagnosisRequest request, List<CodeSnippet> snippets) {
        StringBuilder builder = new StringBuilder();
        builder.append("Service: ").append(request.service()).append("\n");
        if (request.query() != null && !request.query().isBlank()) {
            builder.append("Query: ").append(request.query()).append("\n");
        }
        if (request.errorInfo() != null && !request.errorInfo().isBlank()) {
            builder.append("Error Info: ").append(request.errorInfo()).append("\n");
        }
        if (!snippets.isEmpty()) {
            builder.append("\nRelevant Code Snippets:\n");
            for (CodeSnippet snippet : snippets) {
                builder.append(snippet.filePath())
                    .append(":").append(snippet.startLine()).append("-").append(snippet.endLine())
                    .append("\n```\n")
                    .append(snippet.content())
                    .append("\n```\n\n");
            }
        }
        return builder.toString().trim();
    }

    private DiagnosisResponse parseResponse(String rawResponse, List<CodeSnippet> relatedCode) {
        String cleaned = stripMarkdownFences(rawResponse);
        try {
            ParsedLlmResponse parsed = objectMapper.readValue(cleaned, ParsedLlmResponse.class);
            return new DiagnosisResponse(
                parsed.summary,
                parsed.rootCause,
                parsed.suggestions,
                relatedCode
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LLM response as JSON, using fallback. Response: {}", rawResponse, e);
            return new DiagnosisResponse(
                rawResponse.trim(),
                "",
                List.of(),
                relatedCode
            );
        }
    }

    private String stripMarkdownFences(String text) {
        return text.replaceAll("(?s)^\\s*```(?:json)?\\s*", "")
            .replaceAll("(?s)\\s*```\\s*$", "");
    }

    private record ParsedLlmResponse(
        String summary,
        String rootCause,
        List<String> suggestions
    ) {
    }
}
