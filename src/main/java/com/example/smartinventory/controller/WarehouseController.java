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

import com.example.smartinventory.dto.StockLevelResponse;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.service.StockLevelService;
import com.example.smartinventory.service.WarehouseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for managing {@link Warehouse} resources and reading their stock. */
@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouses", description = "Manage stocking locations and read their stock levels")
public class WarehouseController {

    private final WarehouseService warehouseService;

    private final StockLevelService stockLevelService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a warehouse", description = "Creates a new warehouse. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Warehouse created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content)
    })
    public ResponseEntity<Warehouse> create(@Valid @RequestBody Warehouse warehouse) {
        Warehouse created = warehouseService.create(warehouse);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a warehouse by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Warehouse found"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found", content = @Content)
    })
    public ResponseEntity<Warehouse> findById(
            @Parameter(description = "Identifier of the warehouse") @PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.findById(id));
    }

    @GetMapping
    @Operation(summary = "List all warehouses")
    @ApiResponse(responseCode = "200", description = "Warehouses returned")
    public ResponseEntity<List<Warehouse>> findAll() {
        return ResponseEntity.ok(warehouseService.findAll());
    }

    /**
     * Returns everything stocked in a warehouse, one entry per product held there.
     *
     * @param id identifier of the warehouse
     * @return the stock levels held in that warehouse
     */
    @GetMapping("/{id}/stock")
    @Operation(summary = "List stock held in a warehouse",
            description = "Returns one entry per product stocked in the warehouse.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock levels returned"),
        @ApiResponse(responseCode = "404", description = "Warehouse not found", content = @Content)
    })
    public ResponseEntity<List<StockLevelResponse>> findStock(
            @Parameter(description = "Identifier of the warehouse") @PathVariable Long id) {
        return ResponseEntity.ok(stockLevelService.findByWarehouse(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a warehouse", description = "Replaces an existing warehouse. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Warehouse updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Warehouse not found", content = @Content)
    })
    public ResponseEntity<Warehouse> update(
            @Parameter(description = "Identifier of the warehouse") @PathVariable Long id,
            @Valid @RequestBody Warehouse warehouse) {
        return ResponseEntity.ok(warehouseService.update(id, warehouse));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a warehouse", description = "Deletes a warehouse. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Warehouse deleted"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Warehouse not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifier of the warehouse") @PathVariable Long id) {
        warehouseService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
