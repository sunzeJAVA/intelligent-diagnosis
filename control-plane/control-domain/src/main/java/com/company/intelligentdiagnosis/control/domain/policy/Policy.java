package com.company.intelligentdiagnosis.control.domain.policy;

import java.time.Instant;
import java.util.List;

public record Policy(
    String id,
    String name,
    PolicyType type,
    String regoCode,
    int version,
    PolicyStatus status,
    String createdBy,
    Instant createdAt,
    Instant effectiveAt,
    List<String> appliesTo
) {

    public boolean isEffective() {
        return status == PolicyStatus.ACTIVE
            && effectiveAt != null
            && !effectiveAt.isAfter(Instant.now());
    }
}
