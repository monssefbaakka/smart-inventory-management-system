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
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.ProductBatchRequest;
import com.example.smartinventory.dto.ProductBatchResponse;
import com.example.smartinventory.service.ProductBatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for the batches (lots) a product's stock is made of. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Batches", description = "Track product stock in lots with their own expiry dates")
public class ProductBatchController {

    private final ProductBatchService productBatchService;

    /**
     * Starts tracking a lot of a product. The lot begins empty; stock reaches it through movements
     * naming it.
     *
     * @param productId identifier of the product the lot belongs to
     * @param request   the lot code, optional expiry date and optional warehouse
     * @return the created batch
     */
    @PostMapping("/api/products/{productId}/batches")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Declare a batch",
            description = "Starts tracking a lot of a product. The lot begins empty and is filled by "
                    + "movements naming it. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Batch created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product or warehouse not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Lot code already used by this product",
                content = @Content)
    })
    public ResponseEntity<ProductBatchResponse> create(
            @Parameter(description = "Identifier of the product") @PathVariable Long productId,
            @Valid @RequestBody ProductBatchRequest request) {
        ProductBatchResponse created = ProductBatchResponse.from(productBatchService.create(productId, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Returns every lot of a product, earliest expiry first.
     *
     * @param productId identifier of the product
     * @return the product's lots
     */
    @GetMapping("/api/products/{productId}/batches")
    @Operation(summary = "List a product's batches",
            description = "Returns every lot of the product, whether or not it still holds stock, "
                    + "earliest expiry first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Batches returned"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<List<ProductBatchResponse>> findByProduct(
            @Parameter(description = "Identifier of the product") @PathVariable Long productId) {
        return ResponseEntity.ok(productBatchService.findByProduct(productId).stream()
                .map(ProductBatchResponse::from)
                .toList());
    }

    @GetMapping("/api/batches/{id}")
    @Operation(summary = "Get a batch by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Batch found"),
        @ApiResponse(responseCode = "404", description = "Batch not found", content = @Content)
    })
    public ResponseEntity<ProductBatchResponse> findById(
            @Parameter(description = "Identifier of the batch") @PathVariable Long id) {
        return ResponseEntity.ok(ProductBatchResponse.from(productBatchService.findById(id)));
    }

    /**
     * Stops tracking an empty lot.
     *
     * @param id identifier of the batch
     * @return an empty response
     */
    @DeleteMapping("/api/batches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a batch",
            description = "Stops tracking a lot. Only an empty lot can be deleted. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Batch deleted"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Batch not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Batch still holds stock", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifier of the batch") @PathVariable Long id) {
        productBatchService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
