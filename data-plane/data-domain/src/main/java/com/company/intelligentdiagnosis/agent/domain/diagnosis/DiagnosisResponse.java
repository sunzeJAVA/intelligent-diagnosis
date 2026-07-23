package com.company.intelligentdiagnosis.agent.domain.diagnosis;

import java.util.List;

public record DiagnosisResponse(
    String summary,
    String rootCause,
    List<String> suggestions,
    List<CodeSnippet> relatedCode
) {
}
