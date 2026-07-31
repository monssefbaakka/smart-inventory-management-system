package com.example.smartinventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.AuditLog;

/**
 * Repository for {@link AuditLog} persistence operations.
 *
 * <p>The audit log is read one page at a time through the inherited
 * {@link JpaRepository#findAll(org.springframework.data.domain.Pageable)}; it is the fastest growing
 * table in the schema, so it has no unpaged finder.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}
