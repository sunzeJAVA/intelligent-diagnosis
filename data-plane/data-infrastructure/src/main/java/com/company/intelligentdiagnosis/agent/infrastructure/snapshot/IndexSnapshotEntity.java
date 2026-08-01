package com.company.intelligentdiagnosis.agent.infrastructure.snapshot;

import com.company.intelligentdiagnosis.agent.domain.snapshot.SnapshotStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "index_snapshot")
public class IndexSnapshotEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "repository_id", length = 64, nullable = false)
    private String repositoryId;

    @Column(name = "repository_name", length = 128, nullable = false)
    private String repositoryName;

    @Column(name = "branch", length = 128)
    private String branch;

    @Column(name = "commit_hash", length = 64)
    private String commitHash;

    @Column(name = "previous_commit", length = 64)
    private String previousCommit;

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String commitMessage;

    @Column(name = "author", length = 128)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private SnapshotStatus status;

    @Column(name = "element_count", nullable = false)
    private long elementCount;

    @Column(name = "relation_count", nullable = false)
    private long relationCount;

    @Column(name = "before_snapshot_id", length = 64)
    private String beforeSnapshotId;

    @Column(name = "after_snapshot_id", length = 64)
    private String afterSnapshotId;

    @Column(name = "checksum", length = 256)
    private String checksum;

    @Column(name = "workflow_id", length = 128)
    private String workflowId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "element_ids", columnDefinition = "JSONB")
    private List<String> elementIds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "qdrant_snapshot_path", length = 512)
    private String qdrantSnapshotPath;

    @Column(name = "neo4j_backup_path", length = 512)
    private String neo4jBackupPath;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("validatedAt DESC")
    private List<SnapshotValidationEntity> validations = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getPreviousCommit() {
        return previousCommit;
    }

    public void setPreviousCommit(String previousCommit) {
        this.previousCommit = previousCommit;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public SnapshotStatus getStatus() {
        return status;
    }

    public void setStatus(SnapshotStatus status) {
        this.status = status;
    }

    public long getElementCount() {
        return elementCount;
    }

    public void setElementCount(long elementCount) {
        this.elementCount = elementCount;
    }

    public long getRelationCount() {
        return relationCount;
    }

    public void setRelationCount(long relationCount) {
        this.relationCount = relationCount;
    }

    public String getBeforeSnapshotId() {
        return beforeSnapshotId;
    }

    public void setBeforeSnapshotId(String beforeSnapshotId) {
        this.beforeSnapshotId = beforeSnapshotId;
    }

    public String getAfterSnapshotId() {
        return afterSnapshotId;
    }

    public void setAfterSnapshotId(String afterSnapshotId) {
        this.afterSnapshotId = afterSnapshotId;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public List<String> getElementIds() {
        return elementIds;
    }

    public void setElementIds(List<String> elementIds) {
        this.elementIds = elementIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getQdrantSnapshotPath() {
        return qdrantSnapshotPath;
    }

    public void setQdrantSnapshotPath(String qdrantSnapshotPath) {
        this.qdrantSnapshotPath = qdrantSnapshotPath;
    }

    public String getNeo4jBackupPath() {
        return neo4jBackupPath;
    }

    public void setNeo4jBackupPath(String neo4jBackupPath) {
        this.neo4jBackupPath = neo4jBackupPath;
    }

    public List<SnapshotValidationEntity> getValidations() {
        return validations;
    }

    public void setValidations(List<SnapshotValidationEntity> validations) {
        this.validations = validations;
    }
}
