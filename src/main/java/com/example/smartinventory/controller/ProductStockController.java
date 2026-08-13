package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.StockLevelResponse;
import com.example.smartinventory.dto.StockLevelThresholdRequest;
import com.example.smartinventory.service.StockLevelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoint exposing where a product's stock is held. */
@RestController
@RequestMapping("/api/products/{productId}/stock")
@RequiredArgsConstructor
@Tag(name = "Stock Levels", description = "Per-warehouse stock levels for a product")
public class ProductStockController {

    private final StockLevelService stockLevelService;

    /**
     * Returns a product's stock broken down by the warehouses holding it.
     *
     * @param productId identifier of the product
     * @return the per-warehouse stock levels
     */
    @GetMapping
    @Operation(summary = "List a product's stock by warehouse",
            description = "Returns one entry per warehouse holding the product.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock levels returned"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<List<StockLevelResponse>> findByProduct(
            @Parameter(description = "Identifier of the product") @PathVariable Long productId) {
        return ResponseEntity.ok(stockLevelService.findByProduct(productId));
    }

    /**
     * Sets how low one warehouse may get on this product before it raises an order for itself.
     *
     * @param productId   identifier of the product
     * @param warehouseId identifier of the warehouse
     * @param request     the site's reorder point, or {@code null} to stop measuring it on its own
     * @return the level as the change left it
     */
    @PutMapping("/{warehouseId}/reorder-threshold")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set a warehouse's reorder point for a product",
            description = "Records how low this warehouse may get on the product before a movement "
                    + "through it raises an order for it, delivered there. A null threshold clears "
                    + "it and leaves the site measured as part of the product total. Requires the "
                    + "ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Threshold recorded"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product or warehouse not found", content = @Content)
    })
    public ResponseEntity<StockLevelResponse> setReorderThreshold(
            @Parameter(description = "Identifier of the product") @PathVariable Long productId,
            @Parameter(description = "Identifier of the warehouse") @PathVariable Long warehouseId,
            @Valid @RequestBody StockLevelThresholdRequest request) {
        return ResponseEntity.ok(
                stockLevelService.setReorderThreshold(productId, warehouseId, request.reorderThreshold()));
    }

}
