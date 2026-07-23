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
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.StockMovementRequest;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.service.StockMovementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for recording and viewing product stock movement history. */
@RestController
@RequestMapping("/api/products/{productId}/movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "Record and view stock movement history for products")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    /**
     * Records a stock movement for a product and applies it to the product's quantity.
     *
     * @param productId identifier of the affected product
     * @param request   movement details
     * @return the created movement record
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Record a stock movement",
            description = "Records an IN, OUT or ADJUSTMENT movement and applies it to the product's "
                    + "quantity. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Movement recorded"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "OUT movement exceeds available stock",
                content = @Content)
    })
    public ResponseEntity<StockMovement> record(
            @Parameter(description = "Identifier of the affected product") @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request) {
        StockMovement movement = stockMovementService.record(productId, request.type(), request.quantity(),
                request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(movement);
    }

    @GetMapping
    @Operation(summary = "List stock movements for a product",
            description = "Returns the product's stock movement history, most recent first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Movement history returned"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<List<StockMovement>> findByProduct(
            @Parameter(description = "Identifier of the product") @PathVariable Long productId) {
        return ResponseEntity.ok(stockMovementService.findByProduct(productId));
    }

}
