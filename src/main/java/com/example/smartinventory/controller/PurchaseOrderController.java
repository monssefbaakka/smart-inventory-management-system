package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.PurchaseOrderRequest;
import com.example.smartinventory.model.PurchaseOrder;
import com.example.smartinventory.service.PurchaseOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for managing purchase orders and their lifecycle. */
@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Orders", description = "Create purchase orders and drive their lifecycle")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a purchase order",
            description = "Creates a DRAFT purchase order for a supplier with one or more line items. "
                    + "Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Purchase order created in DRAFT state"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier or product not found", content = @Content)
    })
    public ResponseEntity<PurchaseOrder> create(@Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrder created = purchaseOrderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a purchase order by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Purchase order found"),
        @ApiResponse(responseCode = "404", description = "Purchase order not found", content = @Content)
    })
    public ResponseEntity<PurchaseOrder> findById(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.findById(id));
    }

    /**
     * Lists purchase orders, optionally filtered to a single supplier.
     *
     * @param supplierId optional supplier filter
     * @return the matching purchase orders
     */
    @GetMapping
    @Operation(summary = "List purchase orders",
            description = "Lists all purchase orders, or only those for a supplier when supplierId is given.")
    @ApiResponse(responseCode = "200", description = "Purchase orders returned")
    public ResponseEntity<List<PurchaseOrder>> findAll(
            @Parameter(description = "Optional supplier id to filter by")
            @RequestParam(required = false) Long supplierId) {
        List<PurchaseOrder> orders = supplierId == null
                ? purchaseOrderService.findAll()
                : purchaseOrderService.findBySupplier(supplierId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/place")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Place a purchase order",
            description = "Transitions a DRAFT order to PLACED. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order placed"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Purchase order not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Order is not in a placeable state", content = @Content)
    })
    public ResponseEntity<PurchaseOrder> place(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.place(id));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Receive a purchase order",
            description = "Transitions a PLACED order to RECEIVED, recording an IN stock movement per "
                    + "line item. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order received and stock updated"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Purchase order not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Order is not in a receivable state", content = @Content)
    })
    public ResponseEntity<PurchaseOrder> receive(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.receive(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a purchase order",
            description = "Cancels a DRAFT or PLACED order. A RECEIVED order cannot be cancelled. "
                    + "Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order cancelled"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Purchase order not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "A received order cannot be cancelled", content = @Content)
    })
    public ResponseEntity<PurchaseOrder> cancel(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a purchase order", description = "Deletes a purchase order. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Purchase order deleted"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Purchase order not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id) {
        purchaseOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
