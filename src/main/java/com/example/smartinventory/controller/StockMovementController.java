package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.data.domain.Page;
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
import com.example.smartinventory.dto.StockMovementRequest;
import com.example.smartinventory.dto.StockMovementResponse;
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

    /** The sortable fields as one comma-separated string, for documentation and error messages. */
    static final String SORTABLE_FIELDS_DESCRIPTION = "id, createdAt, quantity, type";

    /** Movement fields a listing may be ordered by. */
    static final List<String> SORTABLE_FIELDS = List.of(SORTABLE_FIELDS_DESCRIPTION.split(", "));

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
                    + "quantity, and to the named warehouse's stock level when one is given. "
                    + "Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Movement recorded"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product or warehouse not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "OUT movement exceeds available stock",
                content = @Content)
    })
    public ResponseEntity<StockMovementResponse> record(
            @Parameter(description = "Identifier of the affected product") @PathVariable Long productId,
            @Valid @RequestBody StockMovementRequest request) {
        StockMovement movement = stockMovementService.record(productId, request.warehouseId(), request.batchId(),
                request.type(), request.quantity(), request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(StockMovementResponse.from(movement));
    }

    /**
     * Returns one page of a product's stock movement history, most recent first by default.
     *
     * @param productId identifier of the product
     * @param page      zero-based index of the page to return
     * @param size      maximum number of movements on the page
     * @param sort      {@code field} or {@code field,direction} to order by
     * @return the requested page of the product's movement history
     */
    @GetMapping
    @Operation(summary = "List stock movements for a product",
            description = "Returns one page of the product's stock movement history, most recent first "
                    + "unless another ordering is asked for. Sortable fields: " + SORTABLE_FIELDS_DESCRIPTION
                    + ". Page size is capped at " + PageRequests.MAX_PAGE_SIZE + ".")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of movement history returned"),
        @ApiResponse(responseCode = "400", description = "Unusable paging or sorting parameter", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<PageResponse<StockMovementResponse>> findByProduct(
            @Parameter(description = "Identifier of the product") @PathVariable Long productId,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, at most " + PageRequests.MAX_PAGE_SIZE)
            @RequestParam(defaultValue = "" + PageRequests.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Ordering as 'field' or 'field,asc|desc'")
            @RequestParam(defaultValue = PageRequests.NEWEST_FIRST) String sort) {
        Page<StockMovement> found =
                stockMovementService.findByProduct(productId, PageRequests.of(page, size, sort, SORTABLE_FIELDS));
        return ResponseEntity.ok(PageResponse.from(found, StockMovementResponse::from));
    }

}
