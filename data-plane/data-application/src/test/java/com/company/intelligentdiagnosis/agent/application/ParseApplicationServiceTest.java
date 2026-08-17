package com.company.intelligentdiagnosis.agent.application;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.ElementKind;
import com.company.intelligentdiagnosis.agent.domain.enrichment.CodeElementEnricher;
import com.company.intelligentdiagnosis.agent.domain.parse.ParseCommand;
import com.company.intelligentdiagnosis.agent.infrastructure.graph.GraphStoreClient;
import com.company.intelligentdiagnosis.agent.infrastructure.parse.ParseWorkerClient;
import com.company.intelligentdiagnosis.agent.infrastructure.vector.VectorStoreClient;
import com.company.intelligentdiagnosis.parse.ParseRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParseApplicationServiceTest {

    @Mock
    private ParseWorkerClient parseWorkerClient;

    @Mock
    private VectorStoreClient vectorStoreClient;

    @Mock
    private GraphStoreClient graphStoreClient;

    @Mock
    private CodeElementEnricher enricher;

    @InjectMocks
    private ParseApplicationService parseApplicationService;

    @Test
    void shouldParseAndIndexCodeElements() {
        ParseCommand command = new ParseCommand(
            "repo",
            "abc123",
            "/tmp/repo",
            List.of("src/Main.java"),
            "java"
        );
        CodeElement element = new CodeElement(
            "repo/src/Main.java#Main",
            ElementKind.CLASS,
            "Main",
            "Main",
            "src/Main.java",
            1,
            5,
            "class Main {}",
            "",
            List.of(),
            List.of(),
            java.util.Map.of(),
            null,
            null
        );
        CodeElement enriched = element.withSummaries("主类", "Main class");
        when(parseWorkerClient.parse("java", buildExpectedRequest(command)))
            .thenReturn(List.of(element));
        when(enricher.enrich(List.of(element))).thenReturn(List.of(enriched));

        List<CodeElement> result = parseApplicationService.parseAndIndex(command);

        assertThat(result).containsExactly(enriched);
        verify(enricher).enrich(List.of(element));
        verify(vectorStoreClient).upsert("repo", List.of(enriched));
        verify(graphStoreClient).buildGraph("repo", "abc123", List.of(enriched));
    }

    private ParseRequest buildExpectedRequest(ParseCommand command) {
        return ParseRequest.newBuilder()
            .setRepository(command.repository())
            .setCommitHash(command.commitHash())
            .setRepoPath(command.repoPath())
            .addAllChangedFiles(command.changedFiles())
            .setLanguage(command.language())
            .build();
    }
}
