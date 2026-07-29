package com.example.smartinventory.model;

import java.time.Instant;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity recording a single auditable mutation of a domain entity. */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning tenant; Hibernate stamps it on insert and filters every query by it. */
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    private String tenantId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @NotNull
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

}
