package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.model.Tenant;
import com.example.smartinventory.service.TenantService;
import com.example.smartinventory.tenant.TenantContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST endpoints for the {@link Tenant} registry.
 *
 * <p>The registry spans the whole installation rather than a single tenant, so managing it is an
 * ADMIN-only operation. Every other endpoint in the API only ever sees the caller's own tenant.
 */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Manage the organisations sharing this installation")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a tenant", description = "Registers a new tenant. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tenant created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "409", description = "Slug already in use", content = @Content)
    })
    public ResponseEntity<Tenant> create(@Valid @RequestBody Tenant tenant) {
        Tenant created = tenantService.create(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all tenants", description = "Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tenants returned"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content)
    })
    public ResponseEntity<List<Tenant>> findAll() {
        return ResponseEntity.ok(tenantService.findAll());
    }

    /**
     * Returns the tenant the calling account belongs to, as resolved for the current request.
     *
     * @return the caller's tenant
     */
    @GetMapping("/current")
    @Operation(summary = "Get the caller's tenant",
            description = "Returns the tenant every request from this account is scoped to.")
    @ApiResponse(responseCode = "200", description = "Tenant returned")
    public ResponseEntity<Tenant> findCurrent() {
        return ResponseEntity.ok(tenantService.findBySlug(TenantContext.getTenantId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a tenant by id", description = "Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tenant found"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Tenant not found", content = @Content)
    })
    public ResponseEntity<Tenant> findById(
            @Parameter(description = "Identifier of the tenant") @PathVariable Long id) {
        return ResponseEntity.ok(tenantService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a tenant",
            description = "Updates a tenant's name and active flag; the slug is immutable. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tenant updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Tenant not found", content = @Content)
    })
    public ResponseEntity<Tenant> update(
            @Parameter(description = "Identifier of the tenant") @PathVariable Long id,
            @Valid @RequestBody Tenant tenant) {
        return ResponseEntity.ok(tenantService.update(id, tenant));
    }

}
