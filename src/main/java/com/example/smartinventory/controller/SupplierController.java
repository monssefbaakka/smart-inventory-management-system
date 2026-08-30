package com.example.smartinventory.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.SupplierReliabilityResponse;
import com.example.smartinventory.dto.SupplierResponse;
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
    @Operation(summary = "Create a supplier",
            description = "Creates a new supplier, optionally naming the warehouse its goods are normally "
                    + "delivered to. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Supplier created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Named default warehouse not found", content = @Content)
    })
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody Supplier supplier) {
        Supplier created = supplierService.create(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(SupplierResponse.from(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a supplier by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Supplier found"),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
    })
    public ResponseEntity<SupplierResponse> findById(
            @Parameter(description = "Identifier of the supplier") @PathVariable Long id) {
        return ResponseEntity.ok(SupplierResponse.from(supplierService.findById(id)));
    }

    @GetMapping("/reliability")
    @Operation(summary = "How well every supplier keeps their dates",
            description = "Reports the same record as /{id}/reliability for every supplier, worst first: by "
                    + "the proportion of deliveries that arrived on time, ascending, with the suppliers "
                    + "having nothing judged last and ties settled by how many orders each was judged on and "
                    + "then by name. Every supplier appears, including the ones nobody has received from. The "
                    + "ranking does not weigh confidence, so ordersJudged says how much each row rests on, and "
                    + "it does not rank on the money, so judgedSpend says what each row is worth. Pass since "
                    + "to rank the suppliers on the deliveries that arrived on or after that day; a supplier "
                    + "with none in the window keeps their row, with nothing judged on it.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Supplier records returned"),
        @ApiResponse(responseCode = "400", description = "since is not an ISO date", content = @Content)
    })
    public ResponseEntity<List<SupplierReliabilityResponse>> reliability(
            @Parameter(description = "Earliest day of arrival to judge, inclusive, as an ISO-8601 date; "
                    + "omitted, the whole record is judged")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since) {
        return ResponseEntity.ok(supplierService.reliability(since));
    }

    @GetMapping("/{id}/reliability")
    @Operation(summary = "How well a supplier keeps their dates",
            description = "Reads the day each of this supplier's orders was due back against the day its "
                    + "goods arrived, over their fulfilled orders carrying both dates. Orders still "
                    + "awaiting delivery, cancelled ones, and ones promised no date are not judged. The "
                    + "average lateness counts only the late orders, and every rate is null when there is "
                    + "nothing to judge. judgedSpend and lateSpend total what those same orders were worth, "
                    + "at ordered quantity times unit price, and are zero rather than null over none of "
                    + "them. Pass since to judge only the deliveries that arrived on or after that day, for "
                    + "the supplier's recent record rather than their whole one.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Supplier record returned"),
        @ApiResponse(responseCode = "400", description = "since is not an ISO date", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier not found", content = @Content)
    })
    public ResponseEntity<SupplierReliabilityResponse> reliability(
            @Parameter(description = "Identifier of the supplier") @PathVariable Long id,
            @Parameter(description = "Earliest day of arrival to judge, inclusive, as an ISO-8601 date; "
                    + "omitted, the whole record is judged")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since) {
        return ResponseEntity.ok(supplierService.reliability(id, since));
    }

    @GetMapping
    @Operation(summary = "List all suppliers")
    @ApiResponse(responseCode = "200", description = "Suppliers returned")
    public ResponseEntity<List<SupplierResponse>> findAll() {
        return ResponseEntity.ok(supplierService.findAll().stream().map(SupplierResponse::from).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a supplier", description = "Replaces an existing supplier. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Supplier updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier or named default warehouse not found",
                content = @Content)
    })
    public ResponseEntity<SupplierResponse> update(
            @Parameter(description = "Identifier of the supplier") @PathVariable Long id,
            @Valid @RequestBody Supplier supplier) {
        return ResponseEntity.ok(SupplierResponse.from(supplierService.update(id, supplier)));
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
