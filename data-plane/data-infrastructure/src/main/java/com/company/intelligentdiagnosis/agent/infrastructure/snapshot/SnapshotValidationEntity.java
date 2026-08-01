package com.company.intelligentdiagnosis.agent.infrastructure.snapshot;

import com.company.intelligentdiagnosis.agent.domain.snapshot.ValidationResult;
import com.company.intelligentdiagnosis.agent.domain.snapshot.ValidationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "snapshot_validation")
public class SnapshotValidationEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private IndexSnapshotEntity snapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_type", length = 32, nullable = false)
    private ValidationType validationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 32, nullable = false)
    private ValidationResult result;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IndexSnapshotEntity getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(IndexSnapshotEntity snapshot) {
        this.snapshot = snapshot;
    }

    public ValidationType getValidationType() {
        return validationType;
    }

    public void setValidationType(ValidationType validationType) {
        this.validationType = validationType;
    }

    public ValidationResult getResult() {
        return result;
    }

    public void setResult(ValidationResult result) {
        this.result = result;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }
}
