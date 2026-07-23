package com.example.smartinventory.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.model.Product;
import com.example.smartinventory.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for managing {@link Product} resources. */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "CRUD operations for inventory products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a product", description = "Creates a new product. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content)
    })
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        Product created = productService.create(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List low-stock products",
            description = "Returns products whose quantity is at or below their reorder threshold.")
    @ApiResponse(responseCode = "200", description = "Low-stock products returned")
    public ResponseEntity<List<Product>> findLowStock() {
        return ResponseEntity.ok(productService.findLowStockProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<Product> findById(
            @Parameter(description = "Identifier of the product") @PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @GetMapping
    @Operation(summary = "List all products")
    @ApiResponse(responseCode = "200", description = "Products returned")
    public ResponseEntity<List<Product>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product", description = "Replaces an existing product. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<Product> update(
            @Parameter(description = "Identifier of the product") @PathVariable Long id,
            @Valid @RequestBody Product product) {
        return ResponseEntity.ok(productService.update(id, product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product", description = "Deletes a product. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product deleted"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifier of the product") @PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
