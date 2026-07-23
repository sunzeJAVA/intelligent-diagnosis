package com.company.intelligentdiagnosis.control.infrastructure.audit;

import com.company.intelligentdiagnosis.control.domain.audit.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<AuditEntry, String> {

    List<AuditEntry> findByTimestampBefore(Instant cutoff);
}
