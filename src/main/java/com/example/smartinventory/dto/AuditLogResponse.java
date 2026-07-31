package com.example.smartinventory.dto;

import java.time.Instant;

import com.example.smartinventory.model.AuditAction;
import com.example.smartinventory.model.AuditLog;

import io.swagger.v3.oas.annotations.media.Schema;

/** An audit entry as returned by the API, without the tenant discriminator it is stored against. */
@Schema(description = "One recorded mutation of a domain entity")
public record AuditLogResponse(

        @Schema(description = "Identifier of the audit entry", example = "1")
        Long id,

        @Schema(description = "Simple name of the mutated entity type", example = "Product")
        String entityType,

        @Schema(description = "Identifier of the mutated entity", example = "42")
        Long entityId,

        @Schema(description = "Kind of mutation performed", example = "UPDATE")
        AuditAction action,

        @Schema(description = "Account that performed the mutation", example = "buyer@acme.example")
        String username,

        @Schema(description = "When the mutation was recorded")
        Instant createdAt) {

    /**
     * Flattens a persisted audit entry into its response form.
     *
     * @param entry the entry to convert
     * @return the response payload
     */
    public static AuditLogResponse from(AuditLog entry) {
        return new AuditLogResponse(
                entry.getId(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getAction(),
                entry.getUsername(),
                entry.getCreatedAt());
    }

}
