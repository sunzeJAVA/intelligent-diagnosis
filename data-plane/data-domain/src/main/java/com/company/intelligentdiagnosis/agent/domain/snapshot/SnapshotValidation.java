package com.company.intelligentdiagnosis.agent.domain.snapshot;

import java.time.Instant;

public record SnapshotValidation(
    String id,
    String snapshotId,
    ValidationType type,
    ValidationResult result,
    String details,
    Instant validatedAt
) {
}
