package com.example.smartinventory.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.model.AuditAction;
import com.example.smartinventory.model.AuditLog;
import com.example.smartinventory.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

/** Service recording and exposing audit-log entries for domain mutations. */
@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {

    /** Username recorded when no authenticated principal is present. */
    static final String ANONYMOUS = "anonymous";

    private final AuditLogRepository auditLogRepository;

    /**
     * Records an audit entry for a mutation, resolving the acting username from the current
     * security context (falling back to {@code anonymous} when unauthenticated).
     *
     * @param entityType simple name of the mutated entity type
     * @param entityId   identifier of the mutated entity
     * @param action     kind of mutation performed
     * @return the persisted audit entry
     */
    public AuditLog record(String entityType, Long entityId, AuditAction action) {
        AuditLog entry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .username(currentUsername())
                .build();
        return auditLogRepository.save(entry);
    }

    /**
     * Returns one page of the audit log.
     *
     * @param pageable the page to return and the order to return it in
     * @return the requested page of audit entries
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            return ANONYMOUS;
        }
        return authentication.getName();
    }

}
