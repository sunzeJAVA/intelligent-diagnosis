package com.company.intelligentdiagnosis.control.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEntryRepository extends JpaRepository<AuditEntryEntity, String> {

    List<AuditEntryEntity> findByResourceOrderByTimestampDesc(String resource);

    List<AuditEntryEntity> findByUserIdOrderByTimestampDesc(String userId);
}
