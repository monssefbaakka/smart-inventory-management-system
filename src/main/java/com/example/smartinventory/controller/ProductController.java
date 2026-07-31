package com.example.smartinventory.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.PageResponse;
import com.example.smartinventory.dto.ProductResponse;
import com.example.smartinventory.dto.ProductSearchCriteria;
import com.example.smartinventory.exception.InvalidQueryParameterException;
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

    /** Page size used when the caller names none. */
    static final int DEFAULT_PAGE_SIZE = 20;

    /** Largest page a caller may ask for, so no single call can pull the whole catalogue. */
    static final int MAX_PAGE_SIZE = 100;

    /** Ordering used when the caller names none. */
    static final String DEFAULT_SORT = "id,asc";

    /** Product fields a listing may be ordered by, in the order they are reported to the caller. */
    static final List<String> SORTABLE_FIELDS = List.of(
            "id", "name", "sku", "price", "quantity", "reorderThreshold", "createdAt", "updatedAt");

    /** The sortable fields as one comma-separated string, for documentation and error messages. */
    static final String SORTABLE_FIELDS_DESCRIPTION =
            "id, name, sku, price, quantity, reorderThreshold, createdAt, updatedAt";

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

    /**
     * Returns one page of products, narrowed by the filters the caller supplied and ordered as it
     * asked. Filters left unset place no restriction, and those supplied are combined with AND.
     *
     * @param search     free text matched case-insensitively against the product name and SKU
     * @param categoryId keeps only products in this category
     * @param supplierId keeps only products from this supplier
     * @param minPrice   keeps only products priced at or above this amount
     * @param maxPrice   keeps only products priced at or below this amount
     * @param lowStock   keeps only products at or below their reorder threshold
     * @param page       zero-based index of the page to return
     * @param size       maximum number of products on the page
     * @param sort       {@code field} or {@code field,direction} to order by
     * @return the requested page of products
     */
    @GetMapping
    @Operation(summary = "List products",
            description = "Returns one page of products. Every filter is optional and they combine with AND. "
                    + "Sortable fields: " + SORTABLE_FIELDS_DESCRIPTION + ". Page size is capped at "
                    + MAX_PAGE_SIZE + ".")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of products returned"),
        @ApiResponse(responseCode = "400", description = "Unusable paging, sorting or filter parameter",
                content = @Content)
    })
    public ResponseEntity<PageResponse<ProductResponse>> search(
            @Parameter(description = "Free text matched against name and SKU") @RequestParam(required = false)
            String search,
            @Parameter(description = "Identifier of the category to keep") @RequestParam(required = false)
            Long categoryId,
            @Parameter(description = "Identifier of the supplier to keep") @RequestParam(required = false)
            Long supplierId,
            @Parameter(description = "Lowest price to keep") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Highest price to keep") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Keep only products at or below their reorder threshold")
            @RequestParam(defaultValue = "false") boolean lowStock,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, at most " + MAX_PAGE_SIZE)
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Ordering as 'field' or 'field,asc|desc'")
            @RequestParam(defaultValue = DEFAULT_SORT) String sort) {
        ProductSearchCriteria criteria =
                new ProductSearchCriteria(search, categoryId, supplierId, minPrice, maxPrice, lowStock);
        Page<Product> found = productService.search(criteria, pageRequest(page, size, sort));
        return ResponseEntity.ok(PageResponse.from(found, ProductResponse::from));
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

    /**
     * Turns the paging parameters into a page request, rejecting anything the endpoint will not
     * serve rather than letting it reach the persistence layer.
     */
    private static PageRequest pageRequest(int page, int size, String sort) {
        if (page < 0) {
            throw new InvalidQueryParameterException("page must not be negative, but was " + page);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidQueryParameterException(
                    "size must be between 1 and " + MAX_PAGE_SIZE + ", but was " + size);
        }
        return PageRequest.of(page, size, parseSort(sort));
    }

    /**
     * Parses a {@code field} or {@code field,direction} ordering, accepting only fields the listing
     * can actually be ordered by.
     */
    private static Sort parseSort(String sort) {
        String[] parts = sort.split(",", 2);
        String field = parts[0].strip();
        if (!SORTABLE_FIELDS.contains(field)) {
            throw new InvalidQueryParameterException(
                    "Cannot sort by '" + field + "'; sortable fields are " + SORTABLE_FIELDS_DESCRIPTION);
        }
        if (parts.length == 1 || parts[1].isBlank()) {
            return Sort.by(Sort.Direction.ASC, field);
        }
        String direction = parts[1].strip();
        return Sort.by(Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new InvalidQueryParameterException(
                        "Unknown sort direction '" + direction + "'; use asc or desc")), field);
    }

    private ResponseEntity<byte[]> png(byte[] image) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

}
