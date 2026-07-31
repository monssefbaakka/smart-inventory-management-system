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

import com.example.smartinventory.dto.PageRequests;
import com.example.smartinventory.dto.PageResponse;
import com.example.smartinventory.dto.PurchaseOrderRequest;
import com.example.smartinventory.dto.PurchaseOrderResponse;
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

    /** The sortable fields as one comma-separated string, for documentation and error messages. */
    static final String SORTABLE_FIELDS_DESCRIPTION = "id, createdAt, updatedAt, status";

    /** Order fields a listing may be ordered by. */
    static final List<String> SORTABLE_FIELDS = List.of(SORTABLE_FIELDS_DESCRIPTION.split(", "));

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
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrder created = purchaseOrderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseOrderResponse.from(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a purchase order by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Purchase order found"),
        @ApiResponse(responseCode = "404", description = "Purchase order not found", content = @Content)
    })
    public ResponseEntity<PurchaseOrderResponse> findById(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id) {
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.findById(id)));
    }

    /**
     * Returns one page of purchase orders, most recent first by default, optionally filtered to a
     * single supplier.
     *
     * @param supplierId optional supplier filter
     * @param page       zero-based index of the page to return
     * @param size       maximum number of orders on the page
     * @param sort       {@code field} or {@code field,direction} to order by
     * @return the requested page of matching orders
     */
    @GetMapping
    @Operation(summary = "List purchase orders",
            description = "Returns one page of purchase orders, most recent first unless another ordering is "
                    + "asked for, and only those for a supplier when supplierId is given. Sortable fields: "
                    + SORTABLE_FIELDS_DESCRIPTION + ". Page size is capped at " + PageRequests.MAX_PAGE_SIZE + ".")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of purchase orders returned"),
        @ApiResponse(responseCode = "400", description = "Unusable paging or sorting parameter", content = @Content),
        @ApiResponse(responseCode = "404", description = "Filtered supplier not found", content = @Content)
    })
    public ResponseEntity<PageResponse<PurchaseOrderResponse>> findAll(
            @Parameter(description = "Optional supplier id to filter by")
            @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, at most " + PageRequests.MAX_PAGE_SIZE)
            @RequestParam(defaultValue = "" + PageRequests.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Ordering as 'field' or 'field,asc|desc'")
            @RequestParam(defaultValue = PageRequests.NEWEST_FIRST) String sort) {
        return ResponseEntity.ok(PageResponse.from(
                purchaseOrderService.find(supplierId, PageRequests.of(page, size, sort, SORTABLE_FIELDS))));
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
    public ResponseEntity<PurchaseOrderResponse> place(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id) {
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.place(id)));
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
    public ResponseEntity<PurchaseOrderResponse> receive(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id) {
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.receive(id)));
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
    public ResponseEntity<PurchaseOrderResponse> cancel(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id) {
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.cancel(id)));
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
