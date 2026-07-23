package com.company.intelligentdiagnosis.agent.domain.diagnosis;

import java.util.List;

public interface CodeRetriever {

    List<CodeSnippet> retrieve(DiagnosisRequest request);
}
