package com.company.intelligentdiagnosis.agent.infrastructure.snapshot;

import com.company.intelligentdiagnosis.agent.domain.CodeElement;
import com.company.intelligentdiagnosis.agent.domain.snapshot.IndexSnapshot;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotRepository;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotStatus;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotValidation;
import com.company.intelligentdiagnosis.agent.domain.snapshot.ValidationResult;
import com.company.intelligentdiagnosis.agent.domain.snapshot.ValidationType;
import com.company.intelligentdiagnosis.agent.domain.workflow.GitPushEvent;
import com.company.intelligentdiagnosis.agent.infrastructure.backup.Neo4jBackupClient;
import com.company.intelligentdiagnosis.agent.infrastructure.backup.QdrantSnapshotClient;
import com.company.intelligentdiagnosis.agent.infrastructure.graph.GraphStoreClient;
import com.company.intelligentdiagnosis.agent.infrastructure.vector.VectorStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SnapshotApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotApplicationService.class);

    private final SnapshotRepository snapshotRepository;
    private final VectorStoreClient vectorStoreClient;
    private final GraphStoreClient graphStoreClient;
    private final QdrantSnapshotClient qdrantSnapshotClient;
    private final Neo4jBackupClient neo4jBackupClient;

    public SnapshotApplicationService(SnapshotRepository snapshotRepository,
                                      VectorStoreClient vectorStoreClient,
                                      GraphStoreClient graphStoreClient,
                                      QdrantSnapshotClient qdrantSnapshotClient,
                                      Neo4jBackupClient neo4jBackupClient) {
        this.snapshotRepository = snapshotRepository;
        this.vectorStoreClient = vectorStoreClient;
        this.graphStoreClient = graphStoreClient;
        this.qdrantSnapshotClient = qdrantSnapshotClient;
        this.neo4jBackupClient = neo4jBackupClient;
    }

    public IndexSnapshot createPreSnapshot(GitPushEvent event, String workflowId) {
        String snapshotId = UUID.randomUUID().toString();
        IndexSnapshot snapshot = new IndexSnapshot(
            snapshotId,
            event.repositoryId(),
            event.repositoryName(),
            event.branch(),
            event.commitHash(),
            event.previousCommit(),
            event.commitMessage(),
            event.author(),
            SnapshotStatus.CREATING,
            0L,
            0L,
            null,
            null,
            checksum(snapshotId + event.commitHash() + Instant.now()),
            workflowId,
            Instant.now(),
            null,
            List.of(),
            new ArrayList<>(),
            null,
            null
        );
        return snapshotRepository.save(snapshot);
    }

    public IndexSnapshot recordSandboxIndex(GitPushEvent event, String snapshotId,
                                            List<CodeElement> elements, List<String> elementIds) {
        IndexSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));

        vectorStoreClient.upsertToSandbox(event.repositoryName(), elements);
        graphStoreClient.buildSandboxGraph(event.repositoryName(), event.commitHash(), elements);

        long relationCount = elements.stream()
            .mapToLong(e -> e.relations() != null ? e.relations().size() : 0)
            .sum();

        IndexSnapshot updated = new IndexSnapshot(
            snapshot.id(),
            snapshot.repositoryId(),
            snapshot.repositoryName(),
            snapshot.branch(),
            snapshot.commitHash(),
            snapshot.previousCommit(),
            snapshot.commitMessage(),
            snapshot.author(),
            SnapshotStatus.VALIDATING,
            elements.size(),
            relationCount,
            snapshot.beforeSnapshotId(),
            snapshot.afterSnapshotId(),
            snapshot.checksum(),
            snapshot.workflowId(),
            snapshot.createdAt(),
            null,
            elementIds,
            snapshot.validations(),
            snapshot.qdrantSnapshotPath(),
            snapshot.neo4jBackupPath()
        );
        return snapshotRepository.save(updated);
    }

    public IndexSnapshot validateSnapshot(String snapshotId) {
        IndexSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));

        List<SnapshotValidation> validations = new ArrayList<>();

        validations.add(validateIntegrity(snapshot));
        validations.add(validateConsistency(snapshot));

        boolean allPassed = validations.stream().allMatch(v -> v.result() == ValidationResult.PASSED);
        SnapshotStatus newStatus = allPassed ? SnapshotStatus.VALIDATING : SnapshotStatus.FAILED;

        IndexSnapshot updated = snapshot.withStatus(newStatus).withValidations(validations);
        return snapshotRepository.save(updated);
    }

    public IndexSnapshot promoteSnapshot(String snapshotId) {
        IndexSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));

        vectorStoreClient.promoteSandboxToProduction(snapshot.repositoryName());
        graphStoreClient.promoteSandboxToProduction(snapshot.repositoryName());

        String qdrantPath = qdrantSnapshotClient.createSnapshot(snapshot.repositoryName(), snapshotId);
        String neo4jPath = neo4jBackupClient.createBackup(snapshot.repositoryName(), snapshotId);

        IndexSnapshot promoted = new IndexSnapshot(
            snapshot.id(),
            snapshot.repositoryId(),
            snapshot.repositoryName(),
            snapshot.branch(),
            snapshot.commitHash(),
            snapshot.previousCommit(),
            snapshot.commitMessage(),
            snapshot.author(),
            SnapshotStatus.PROMOTED,
            snapshot.elementCount(),
            snapshot.relationCount(),
            snapshot.beforeSnapshotId(),
            snapshot.afterSnapshotId(),
            snapshot.checksum(),
            snapshot.workflowId(),
            snapshot.createdAt(),
            Instant.now(),
            snapshot.elementIds(),
            snapshot.validations(),
            qdrantPath,
            neo4jPath
        );
        log.info("Promoted snapshot {} for repository {} with physical backups: qdrant={}, neo4j={}",
            snapshotId, snapshot.repositoryName(), qdrantPath, neo4jPath);
        return snapshotRepository.save(promoted);
    }

    public void rollbackTo(String snapshotId) {
        IndexSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));

        vectorStoreClient.deleteByRepository(snapshot.repositoryName());
        graphStoreClient.deleteByRepository(snapshot.repositoryName());
        graphStoreClient.deleteSandboxByRepository(snapshot.repositoryName());

        IndexSnapshot rolledBack = snapshot.withStatus(SnapshotStatus.ROLLED_BACK);
        snapshotRepository.save(rolledBack);
        log.info("Rolled back to snapshot {} for repository {}", snapshotId, snapshot.repositoryName());
    }

    public Optional<IndexSnapshot> findLatestPromoted(String repositoryName) {
        return snapshotRepository.findLatestPromotedByRepositoryName(repositoryName);
    }

    /**
     * 从物理快照恢复指定仓库的索引数据
     */
    public IndexSnapshot restoreSnapshot(String snapshotId) {
        IndexSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));

        if (snapshot.qdrantSnapshotPath() == null || snapshot.neo4jBackupPath() == null) {
            throw new IllegalStateException("Snapshot does not have physical backups: " + snapshotId);
        }

        String repositoryName = snapshot.repositoryName();

        vectorStoreClient.deleteByRepository(repositoryName);
        graphStoreClient.deleteByRepository(repositoryName);
        graphStoreClient.deleteSandboxByRepository(repositoryName);

        qdrantSnapshotClient.restoreSnapshot(repositoryName, snapshotId);
        neo4jBackupClient.restoreBackup(repositoryName, snapshotId);

        IndexSnapshot restored = snapshot
            .withStatus(SnapshotStatus.PROMOTED)
            .withCompletedAt(Instant.now());
        log.info("Physically restored repository {} to snapshot {}", repositoryName, snapshotId);
        return snapshotRepository.save(restored);
    }

    private SnapshotValidation validateIntegrity(IndexSnapshot snapshot) {
        try {
            long vectorCount = vectorStoreClient.countByRepository(
                "code-elements-sandbox",
                snapshot.repositoryName()
            );
            boolean passed = vectorCount == snapshot.elementCount();
            return new SnapshotValidation(
                UUID.randomUUID().toString(),
                snapshot.id(),
                ValidationType.INTEGRITY,
                passed ? ValidationResult.PASSED : ValidationResult.FAILED,
                "vectorCount=" + vectorCount + ", expected=" + snapshot.elementCount(),
                Instant.now()
            );
        } catch (Exception e) {
            return new SnapshotValidation(
                UUID.randomUUID().toString(),
                snapshot.id(),
                ValidationType.INTEGRITY,
                ValidationResult.FAILED,
                e.getMessage(),
                Instant.now()
            );
        }
    }

    private SnapshotValidation validateConsistency(IndexSnapshot snapshot) {
        try {
            long graphNodeCount = graphStoreClient.countNodes(snapshot.repositoryName());
            boolean passed = graphNodeCount == snapshot.elementCount();
            return new SnapshotValidation(
                UUID.randomUUID().toString(),
                snapshot.id(),
                ValidationType.CONSISTENCY,
                passed ? ValidationResult.PASSED : ValidationResult.FAILED,
                "graphNodeCount=" + graphNodeCount + ", expected=" + snapshot.elementCount(),
                Instant.now()
            );
        } catch (Exception e) {
            return new SnapshotValidation(
                UUID.randomUUID().toString(),
                snapshot.id(),
                ValidationType.CONSISTENCY,
                ValidationResult.FAILED,
                e.getMessage(),
                Instant.now()
            );
        }
    }

    private String checksum(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
