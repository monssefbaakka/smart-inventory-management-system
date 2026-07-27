package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.StockLevelResponse;
import com.example.smartinventory.service.StockLevelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

}
