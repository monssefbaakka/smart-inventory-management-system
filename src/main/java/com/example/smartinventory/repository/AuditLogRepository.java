package com.example.smartinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.smartinventory.model.AuditLog;

/** Repository for {@link AuditLog} persistence operations. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Returns all audit entries ordered most recent first.
     *
     * @return the audit log, newest entries first
     */
    List<AuditLog> findAllByOrderByCreatedAtDesc();

}
