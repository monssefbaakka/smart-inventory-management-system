package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.model.Supplier;
import com.example.smartinventory.service.SupplierService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for managing {@link Supplier} resources. */
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "CRUD operations for suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a supplier", description = "Creates a new supplier. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Supplier created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content)
    })
    public ResponseEntity<Supplier> create(@Valid @RequestBody Supplier supplier) {
        Supplier created = supplierService.create(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a supplier by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Supplier found"),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
    })
    public ResponseEntity<Supplier> findById(
            @Parameter(description = "Identifier of the supplier") @PathVariable Long id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    @GetMapping
    @Operation(summary = "List all suppliers")
    @ApiResponse(responseCode = "200", description = "Suppliers returned")
    public ResponseEntity<List<Supplier>> findAll() {
        return ResponseEntity.ok(supplierService.findAll());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a supplier", description = "Replaces an existing supplier. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Supplier updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
    })
    public ResponseEntity<Supplier> update(
            @Parameter(description = "Identifier of the supplier") @PathVariable Long id,
            @Valid @RequestBody Supplier supplier) {
        return ResponseEntity.ok(supplierService.update(id, supplier));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a supplier", description = "Deletes a supplier. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Supplier deleted"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifier of the supplier") @PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
