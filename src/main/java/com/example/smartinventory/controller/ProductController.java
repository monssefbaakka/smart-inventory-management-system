package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import com.example.smartinventory.dto.ProductResponse;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.service.BarcodeService;
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

    private final BarcodeService barcodeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a product", description = "Creates a new product. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content)
    })
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody Product product) {
        Product created = productService.create(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(created));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List low-stock products",
            description = "Returns products whose quantity is at or below their reorder threshold.")
    @ApiResponse(responseCode = "200", description = "Low-stock products returned")
    public ResponseEntity<List<ProductResponse>> findLowStock() {
        return ResponseEntity.ok(productService.findLowStockProducts().stream().map(ProductResponse::from).toList());
    }

    /**
     * Resolves a scanned barcode to the product carrying it, for scanner-driven lookups.
     *
     * @param barcode the symbol content read from a scanner
     * @return the matching product
     */
    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Look up a product by barcode",
            description = "Resolves a scanned barcode symbol to the product carrying it.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "No product carries that barcode", content = @Content)
    })
    public ResponseEntity<ProductResponse> findByBarcode(
            @Parameter(description = "Scanned barcode symbol content") @PathVariable String barcode) {
        return ResponseEntity.ok(ProductResponse.from(productService.findByBarcode(barcode)));
    }

    /**
     * Renders a printable Code 128 label for a product, encoding its barcode or, when unset, its SKU.
     *
     * @param id identifier of the product
     * @return the PNG image of the barcode
     */
    @GetMapping(value = "/{id}/barcode.png", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Render a product barcode",
            description = "Returns a Code 128 PNG encoding the product barcode, falling back to its SKU.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Barcode image returned"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<byte[]> barcodeImage(
            @Parameter(description = "Identifier of the product") @PathVariable Long id) {
        return png(barcodeService.generateBarcode(productService.symbolContent(id)));
    }

    /**
     * Renders a QR code for a product, encoding its barcode or, when unset, its SKU.
     *
     * @param id identifier of the product
     * @return the PNG image of the QR code
     */
    @GetMapping(value = "/{id}/qrcode.png", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Render a product QR code",
            description = "Returns a QR PNG encoding the product barcode, falling back to its SKU.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "QR image returned"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<byte[]> qrCodeImage(
            @Parameter(description = "Identifier of the product") @PathVariable Long id) {
        return png(barcodeService.generateQrCode(productService.symbolContent(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ProductResponse> findById(
            @Parameter(description = "Identifier of the product") @PathVariable Long id) {
        return ResponseEntity.ok(ProductResponse.from(productService.findById(id)));
    }

    @GetMapping
    @Operation(summary = "List all products")
    @ApiResponse(responseCode = "200", description = "Products returned")
    public ResponseEntity<List<ProductResponse>> findAll() {
        return ResponseEntity.ok(productService.findAll().stream().map(ProductResponse::from).toList());
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
    public ResponseEntity<ProductResponse> update(
            @Parameter(description = "Identifier of the product") @PathVariable Long id,
            @Valid @RequestBody Product product) {
        return ResponseEntity.ok(ProductResponse.from(productService.update(id, product)));
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

    private ResponseEntity<byte[]> png(byte[] image) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

}
