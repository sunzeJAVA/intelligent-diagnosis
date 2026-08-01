package com.company.intelligentdiagnosis.agent.infrastructure.snapshot;

import com.company.intelligentdiagnosis.agent.domain.snapshot.IndexSnapshot;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotRepository;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotStatus;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotValidation;
import com.company.intelligentdiagnosis.agent.domain.snapshot.ValidationResult;
import com.company.intelligentdiagnosis.agent.domain.snapshot.ValidationType;
import com.company.intelligentdiagnosis.agent.infrastructure.graph.GraphStoreClient;
import com.company.intelligentdiagnosis.agent.infrastructure.vector.VectorStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SnapshotIntegrityChecker {

    private static final Logger log = LoggerFactory.getLogger(SnapshotIntegrityChecker.class);

    private final SnapshotRepository snapshotRepository;
    private final VectorStoreClient vectorStoreClient;
    private final GraphStoreClient graphStoreClient;
    private final JpaSnapshotRepository jpaSnapshotRepository;

    public SnapshotIntegrityChecker(SnapshotRepository snapshotRepository,
                                    VectorStoreClient vectorStoreClient,
                                    GraphStoreClient graphStoreClient,
                                    JpaSnapshotRepository jpaSnapshotRepository) {
        this.snapshotRepository = snapshotRepository;
        this.vectorStoreClient = vectorStoreClient;
        this.graphStoreClient = graphStoreClient;
        this.jpaSnapshotRepository = jpaSnapshotRepository;
    }

    @Scheduled(fixedDelayString = "${snapshot.integrity.check-interval:300000}")
    @Transactional
    public void checkPromotedSnapshots() {
        List<IndexSnapshot> snapshots = snapshotRepository.findByStatus(SnapshotStatus.PROMOTED);

        for (IndexSnapshot snapshot : snapshots) {
            try {
                long vectorCount = vectorStoreClient.countByRepository("code-elements", snapshot.repositoryName());
                long graphNodeCount = graphStoreClient.countNodes(snapshot.repositoryName());

                boolean vectorOk = vectorCount == snapshot.elementCount();
                boolean graphOk = graphNodeCount == snapshot.elementCount();

                SnapshotValidation validation = new SnapshotValidation(
                    UUID.randomUUID().toString(),
                    snapshot.id(),
                    ValidationType.INTEGRITY,
                    vectorOk && graphOk ? ValidationResult.PASSED : ValidationResult.FAILED,
                    "vectorCount=" + vectorCount + ", graphNodeCount=" + graphNodeCount
                        + ", expected=" + snapshot.elementCount(),
                    Instant.now()
                );

                IndexSnapshotEntity entity = jpaSnapshotRepository.findEntityById(snapshot.id()).orElse(null);
                if (entity != null) {
                    SnapshotValidationEntity validationEntity = SnapshotMapper.toEntity(validation, entity);
                    entity.getValidations().add(validationEntity);
                    jpaSnapshotRepository.saveEntity(entity);
                }

                log.info("Integrity check completed for snapshot {}: {}", snapshot.id(), validation.result());
            } catch (Exception e) {
                log.error("Failed to check integrity for snapshot {}", snapshot.id(), e);
            }
        }
    }
}
