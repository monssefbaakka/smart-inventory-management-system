package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.AuditLogResponse;
import com.example.smartinventory.dto.PageRequests;
import com.example.smartinventory.dto.PageResponse;
import com.example.smartinventory.model.AuditLog;
import com.example.smartinventory.service.AuditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** REST endpoints exposing the audit log of domain mutations. */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "View the audit trail of domain changes")
public class AuditLogController {

    /** The sortable fields as one comma-separated string, for documentation and error messages. */
    static final String SORTABLE_FIELDS_DESCRIPTION = "id, createdAt, entityType, entityId, action, username";

    /** Audit fields a listing may be ordered by. */
    static final List<String> SORTABLE_FIELDS = List.of(SORTABLE_FIELDS_DESCRIPTION.split(", "));

    private final AuditService auditService;

    /**
     * Returns one page of audit entries, most recent first by default.
     *
     * @param page zero-based index of the page to return
     * @param size maximum number of entries on the page
     * @param sort {@code field} or {@code field,direction} to order by
     * @return the requested page of audit entries
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List audit entries",
            description = "Returns one page of audit entries, most recent first unless another ordering is "
                    + "asked for. Sortable fields: " + SORTABLE_FIELDS_DESCRIPTION + ". Page size is capped at "
                    + PageRequests.MAX_PAGE_SIZE + ". Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of audit entries returned"),
        @ApiResponse(responseCode = "400", description = "Unusable paging or sorting parameter", content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content)
    })
    public ResponseEntity<PageResponse<AuditLogResponse>> findAll(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, at most " + PageRequests.MAX_PAGE_SIZE)
            @RequestParam(defaultValue = "" + PageRequests.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Ordering as 'field' or 'field,asc|desc'")
            @RequestParam(defaultValue = PageRequests.NEWEST_FIRST) String sort) {
        Page<AuditLog> entries = auditService.findAll(PageRequests.of(page, size, sort, SORTABLE_FIELDS));
        return ResponseEntity.ok(PageResponse.from(entries, AuditLogResponse::from));
    }

}
