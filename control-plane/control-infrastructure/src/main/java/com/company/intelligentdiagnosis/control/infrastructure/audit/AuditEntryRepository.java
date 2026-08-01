package com.company.intelligentdiagnosis.control.infrastructure.audit;

import com.company.intelligentdiagnosis.control.domain.audit.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditEntryRepository extends JpaRepository<AuditEntryEntity, String> {

    List<AuditEntryEntity> findByResourceOrderByTimestampDesc(String resource);

    List<AuditEntryEntity> findByUserIdOrderByTimestampDesc(String userId);

    long countByActionAndTimestampAfter(AuditAction action, Instant timestamp);
}
