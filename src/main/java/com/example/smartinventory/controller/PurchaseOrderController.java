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

import com.example.smartinventory.dto.GoodsReceiptRequest;
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
            description = "Creates a DRAFT purchase order for a supplier with one or more line items. The order "
                    + "may name the warehouse the goods are to be delivered to, which a receipt against it books "
                    + "into unless the receipt or one of its lines names another. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Purchase order created in DRAFT state"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Supplier, product or warehouse not found",
                content = @Content)
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
     * @param overdue    keeps only the orders whose goods were due before today
     * @param page       zero-based index of the page to return
     * @param size       maximum number of orders on the page
     * @param sort       {@code field} or {@code field,direction} to order by
     * @return the requested page of matching orders
     */
    @GetMapping
    @Operation(summary = "List purchase orders",
            description = "Returns one page of purchase orders, most recent first unless another ordering is "
                    + "asked for, only those for a supplier when supplierId is given, and only those running "
                    + "late when overdue is true. Sortable fields: "
                    + SORTABLE_FIELDS_DESCRIPTION + ". Page size is capped at " + PageRequests.MAX_PAGE_SIZE + ".")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of purchase orders returned"),
        @ApiResponse(responseCode = "400", description = "Unusable paging or sorting parameter", content = @Content),
        @ApiResponse(responseCode = "404", description = "Filtered supplier not found", content = @Content)
    })
    public ResponseEntity<PageResponse<PurchaseOrderResponse>> findAll(
            @Parameter(description = "Optional supplier id to filter by")
            @RequestParam(required = false) Long supplierId,
            @Parameter(description = "Keep only orders awaiting delivery whose expected delivery date has passed")
            @RequestParam(defaultValue = "false") boolean overdue,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, at most " + PageRequests.MAX_PAGE_SIZE)
            @RequestParam(defaultValue = "" + PageRequests.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Ordering as 'field' or 'field,asc|desc'")
            @RequestParam(defaultValue = PageRequests.NEWEST_FIRST) String sort) {
        return ResponseEntity.ok(PageResponse.from(
                purchaseOrderService.find(supplierId, overdue, PageRequests.of(page, size, sort, SORTABLE_FIELDS))));
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

    /**
     * Receives everything still outstanding on an order, optionally into one warehouse.
     *
     * @param id          identifier of the purchase order
     * @param warehouseId identifier of the location the goods landed in, or {@code null} to deliver
     *                    the order where it says it was to be delivered
     * @return the received order
     */
    @PostMapping("/{id}/receive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Receive a purchase order in full",
            description = "Receives everything still outstanding on a PLACED or PARTIALLY_RECEIVED order, "
                    + "recording an IN stock movement per line and leaving the order RECEIVED. The goods land "
                    + "in warehouseId when one is given, and otherwise in the warehouse the order is to be "
                    + "delivered to, or against the product total only when neither says; no lot is named, "
                    + "which has to be said line by line through /receipts. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order received and stock updated"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Purchase order or warehouse not found",
                content = @Content),
        @ApiResponse(responseCode = "409", description = "Order is not in a receivable state", content = @Content)
    })
    public ResponseEntity<PurchaseOrderResponse> receive(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id,
            @Parameter(description = "Optional warehouse the goods landed in, overriding the one the order is "
                    + "to be delivered to")
            @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.receive(id, warehouseId)));
    }

    /**
     * Books one delivery against an order, taking the stated quantity of each named line into stock
     * at the location and in the lot it says it landed in.
     *
     * @param id      identifier of the purchase order
     * @param request the lines that arrived, how much of each, and where it went
     * @return the order as the delivery left it
     */
    @PostMapping("/{id}/receipts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Book a delivery against a purchase order",
            description = "Receives the stated quantity of each named line item, recording an IN stock movement "
                    + "at the line's unit price. The goods land in the receipt's warehouseId unless the line "
                    + "names its own, or in the warehouse the order is to be delivered to when neither does, "
                    + "and in the lot named by its lotCode, which is created against the product "
                    + "if it does not exist yet. A line may be listed more than once to split a delivery across "
                    + "lots or sites. Lines left out of the request stay outstanding. The order ends "
                    + "up RECEIVED once every line is complete and PARTIALLY_RECEIVED while anything is still "
                    + "to come. A line received past the quantity ordered is rejected and the whole delivery is "
                    + "booked as one transaction. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Delivery booked and stock updated"),
        @ApiResponse(responseCode = "400",
                description = "Validation failed, or a line states an expiry date but no lot code",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Purchase order, line item or warehouse not found",
                content = @Content),
        @ApiResponse(responseCode = "409",
                description = "Order is not in a receivable state, a line would exceed the quantity ordered, or a "
                        + "lot code is stated under a different expiry date",
                content = @Content)
    })
    public ResponseEntity<PurchaseOrderResponse> receiveDelivery(
            @Parameter(description = "Identifier of the purchase order") @PathVariable Long id,
            @Valid @RequestBody GoodsReceiptRequest request) {
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.receive(id, request)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a purchase order",
            description = "Cancels an order that has not been received in full, abandoning whatever is still "
                    + "outstanding. Stock already received against a part-delivered order stays. A RECEIVED "
                    + "order cannot be cancelled. Requires the ADMIN role.")
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
