package com.company.intelligentdiagnosis.agent.application;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.parse.ParseCommand;
import com.company.intelligentdiagnosis.agent.infrastructure.graph.GraphStoreClient;
import com.company.intelligentdiagnosis.agent.infrastructure.parse.ParseWorkerClient;
import com.company.intelligentdiagnosis.agent.infrastructure.vector.VectorStoreClient;
import com.company.intelligentdiagnosis.parse.ParseRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 解析应用服务
 * 协调代码解析和索引流程：发送解析任务 → 接收解析结果 → 向量索引 → 图构建
 */
@Service
public class ParseApplicationService {

    private final ParseWorkerClient parseWorkerClient;
    private final VectorStoreClient vectorStoreClient;
    private final GraphStoreClient graphStoreClient;

    /**
     * 构造函数
     *
     * @param parseWorkerClient 解析工作器客户端
     * @param vectorStoreClient 向量存储客户端
     * @param graphStoreClient  图存储客户端
     */
    public ParseApplicationService(ParseWorkerClient parseWorkerClient,
                                   VectorStoreClient vectorStoreClient,
                                   GraphStoreClient graphStoreClient) {
        this.parseWorkerClient = parseWorkerClient;
        this.vectorStoreClient = vectorStoreClient;
        this.graphStoreClient = graphStoreClient;
    }

    /**
     * 解析并索引代码
     *
     * @param command 解析命令
     * @return 解析后的代码元素列表
     */
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
