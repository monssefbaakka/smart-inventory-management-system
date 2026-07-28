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
     * Lists transfers, most recent first, optionally narrowed to one product or one warehouse.
     *
     * @param productId   identifier of a product to filter by, or {@code null}
     * @param warehouseId identifier of a warehouse to filter by, or {@code null}
     * @return the matching transfers
     */
    @GetMapping
    @Operation(summary = "List stock transfers",
            description = "Returns transfers most recent first. Filter by product, or by a warehouse on "
                    + "either side of the move; product wins when both are given.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfers returned"),
        @ApiResponse(responseCode = "404", description = "Filtered product or warehouse not found",
                content = @Content)
    })
    public ResponseEntity<List<StockTransferResponse>> find(
            @Parameter(description = "Only transfers of this product") @RequestParam(required = false) Long productId,
            @Parameter(description = "Only transfers into or out of this warehouse")
            @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(stockTransferService.find(productId, warehouseId));
    }

}
