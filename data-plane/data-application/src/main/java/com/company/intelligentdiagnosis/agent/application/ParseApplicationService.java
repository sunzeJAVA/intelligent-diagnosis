package com.company.intelligentdiagnosis.agent.application;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.parse.ParseCommand;
import com.company.intelligentdiagnosis.agent.infrastructure.graph.GraphStoreClient;
import com.company.intelligentdiagnosis.agent.infrastructure.parse.ParseWorkerClient;
import com.company.intelligentdiagnosis.agent.infrastructure.vector.VectorStoreClient;
import com.company.intelligentdiagnosis.parse.ParseRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParseApplicationService {

    private final ParseWorkerClient parseWorkerClient;
    private final VectorStoreClient vectorStoreClient;
    private final GraphStoreClient graphStoreClient;

    public ParseApplicationService(ParseWorkerClient parseWorkerClient,
                                   VectorStoreClient vectorStoreClient,
                                   GraphStoreClient graphStoreClient) {
        this.parseWorkerClient = parseWorkerClient;
        this.vectorStoreClient = vectorStoreClient;
        this.graphStoreClient = graphStoreClient;
    }

    public List<CodeElement> parseAndIndex(ParseCommand command) {
        ParseRequest request = ParseRequest.newBuilder()
            .setRepository(command.repository())
            .setCommitHash(command.commitHash())
            .setRepoPath(command.repoPath())
            .addAllChangedFiles(command.changedFiles())
            .setLanguage(command.language())
            .build();

        List<CodeElement> elements = parseWorkerClient.parse(command.language(), request);

        vectorStoreClient.upsert(command.repository(), elements);
        graphStoreClient.buildGraph(command.repository(), command.commitHash(), elements);

        return elements;
    }
}
