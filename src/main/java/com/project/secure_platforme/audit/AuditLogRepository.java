package com.project.secure_platforme.audit;

import org.springframework.data.repository.Repository;

import java.util.List;

public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findTop50ByUsernameOrderByTimestampDesc(String username);

    List<AuditLog> findTop50ByEventTypeOrderByTimestampDesc(String eventType);
}

