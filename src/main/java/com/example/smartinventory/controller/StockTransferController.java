package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.PageRequests;
import com.example.smartinventory.dto.PageResponse;
import com.example.smartinventory.dto.StockTransferRequest;
import com.example.smartinventory.dto.StockTransferResponse;
import com.example.smartinventory.service.StockTransferService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for moving stock between warehouses and reading the transfer history. */
@RestController
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
@Tag(name = "Stock Transfers", description = "Move stock between warehouses and read past moves")
public class StockTransferController {

    /** The sortable fields as one comma-separated string, for documentation and error messages. */
    static final String SORTABLE_FIELDS_DESCRIPTION = "id, createdAt, quantity";

    /** Transfer fields a listing may be ordered by. */
    static final List<String> SORTABLE_FIELDS = List.of(SORTABLE_FIELDS_DESCRIPTION.split(", "));

    private final StockTransferService stockTransferService;

    /**
     * Moves stock from one warehouse to another, leaving the product's overall quantity unchanged.
     *
     * @param request the transfer details
     * @return the recorded transfer
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Transfer stock between warehouses",
            description = "Moves units from the source warehouse to the destination warehouse. The two "
                    + "stock levels change by equal and opposite amounts and the product's overall "
                    + "quantity is unchanged. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transfer recorded"),
        @ApiResponse(responseCode = "400",
                description = "Validation failed, both sides name the same warehouse, or the destination "
                        + "is inactive", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product or warehouse not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Source warehouse holds too little stock",
                content = @Content)
    })
    public ResponseEntity<StockTransferResponse> transfer(@Valid @RequestBody StockTransferRequest request) {
        StockTransferResponse created = stockTransferService.transfer(request.productId(),
                request.sourceWarehouseId(), request.destinationWarehouseId(), request.quantity(), request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a stock transfer by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfer found"),
        @ApiResponse(responseCode = "404", description = "Transfer not found", content = @Content)
    })
    public ResponseEntity<StockTransferResponse> findById(
            @Parameter(description = "Identifier of the transfer") @PathVariable Long id) {
        return ResponseEntity.ok(stockTransferService.findById(id));
    }

    /**
     * Returns one page of transfers, most recent first by default, optionally narrowed to one
     * product or one warehouse.
     *
     * @param productId   identifier of a product to filter by, or {@code null}
     * @param warehouseId identifier of a warehouse to filter by, or {@code null}
     * @param page        zero-based index of the page to return
     * @param size        maximum number of transfers on the page
     * @param sort        {@code field} or {@code field,direction} to order by
     * @return the requested page of matching transfers
     */
    @GetMapping
    @Operation(summary = "List stock transfers",
            description = "Returns one page of transfers, most recent first unless another ordering is asked "
                    + "for. Filter by product, or by a warehouse on either side of the move; product wins "
                    + "when both are given. Sortable fields: " + SORTABLE_FIELDS_DESCRIPTION
                    + ". Page size is capped at " + PageRequests.MAX_PAGE_SIZE + ".")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of transfers returned"),
        @ApiResponse(responseCode = "400", description = "Unusable paging or sorting parameter", content = @Content),
        @ApiResponse(responseCode = "404", description = "Filtered product or warehouse not found",
                content = @Content)
    })
    public ResponseEntity<PageResponse<StockTransferResponse>> find(
            @Parameter(description = "Only transfers of this product") @RequestParam(required = false) Long productId,
            @Parameter(description = "Only transfers into or out of this warehouse")
            @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, at most " + PageRequests.MAX_PAGE_SIZE)
            @RequestParam(defaultValue = "" + PageRequests.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Ordering as 'field' or 'field,asc|desc'")
            @RequestParam(defaultValue = PageRequests.NEWEST_FIRST) String sort) {
        return ResponseEntity.ok(PageResponse.from(stockTransferService.find(productId, warehouseId,
                PageRequests.of(page, size, sort, SORTABLE_FIELDS))));
    }

}
