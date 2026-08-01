package com.company.intelligentdiagnosis.agent.infrastructure.snapshot;

import com.company.intelligentdiagnosis.agent.domain.snapshot.IndexSnapshot;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotRepository;
import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotStatus;
import com.company.intelligentdiagnosis.agent.infrastructure.backup.Neo4jBackupClient;
import com.company.intelligentdiagnosis.agent.infrastructure.backup.QdrantSnapshotClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自动物理快照备份任务
 * 定期为已提升（PROMOTED）但尚未生成物理备份的快照补全 Qdrant/Neo4j 备份
 */
@Component
public class SnapshotBackupJob {

    private static final Logger log = LoggerFactory.getLogger(SnapshotBackupJob.class);

    private final SnapshotRepository snapshotRepository;
    private final QdrantSnapshotClient qdrantSnapshotClient;
    private final Neo4jBackupClient neo4jBackupClient;

    public SnapshotBackupJob(SnapshotRepository snapshotRepository,
                             QdrantSnapshotClient qdrantSnapshotClient,
                             Neo4jBackupClient neo4jBackupClient) {
        this.snapshotRepository = snapshotRepository;
        this.qdrantSnapshotClient = qdrantSnapshotClient;
        this.neo4jBackupClient = neo4jBackupClient;
    }

    /**
     * 默认每 6 小时检查一次缺失的物理备份
     */
    @Scheduled(cron = "${snapshot.backup.cron:0 0 */6 * * *}")
    public void ensurePhysicalBackups() {
        List<IndexSnapshot> missingBackups = snapshotRepository.findByStatus(SnapshotStatus.PROMOTED).stream()
            .filter(s -> s.qdrantSnapshotPath() == null || s.neo4jBackupPath() == null)
            .toList();

        if (missingBackups.isEmpty()) {
            log.debug("No promoted snapshots missing physical backups");
            return;
        }

        log.info("Ensuring physical backups for {} promoted snapshots", missingBackups.size());
        for (IndexSnapshot snapshot : missingBackups) {
            try {
                String qdrantPath = qdrantSnapshotClient.createSnapshot(snapshot.repositoryName(), snapshot.id());
                String neo4jPath = neo4jBackupClient.createBackup(snapshot.repositoryName(), snapshot.id());
                IndexSnapshot updated = snapshot.withBackupPaths(qdrantPath, neo4jPath);
                snapshotRepository.save(updated);
                log.info("Created physical backups for snapshot {}: qdrant={}, neo4j={}",
                    snapshot.id(), qdrantPath, neo4jPath);
            } catch (Exception e) {
                log.error("Failed to create physical backups for snapshot {}: {}", snapshot.id(), e.getMessage(), e);
            }
        }
    }
}
