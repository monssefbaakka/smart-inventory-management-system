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

import com.example.smartinventory.dto.StockCountLineRequest;
import com.example.smartinventory.dto.StockCountRequest;
import com.example.smartinventory.dto.StockCountResponse;
import com.example.smartinventory.model.StockCountStatus;
import com.example.smartinventory.service.StockCountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** REST endpoints for running stocktakes against a warehouse. */
@RestController
@RequestMapping("/api/stock-counts")
@RequiredArgsConstructor
@Tag(name = "Stock Counts", description = "Count what a warehouse holds and reconcile the result")
public class StockCountController {

    private final StockCountService stockCountService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Open a stock count",
            description = "Opens a DRAFT count against a warehouse. No stock changes until the count "
                    + "is completed. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Count opened"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Warehouse not found", content = @Content)
    })
    public ResponseEntity<StockCountResponse> open(@Valid @RequestBody StockCountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockCountService.open(request));
    }

    /**
     * Records what was found on the shelf for one product.
     *
     * @param id      identifier of the count
     * @param request the counted product and quantity
     * @return the count including the new or updated line
     */
    @PostMapping("/{id}/lines")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Record a counted line",
            description = "Records the quantity found for one product. Counting the same product again "
                    + "replaces the earlier line. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Line recorded"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Count or product not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Count is no longer DRAFT", content = @Content)
    })
    public ResponseEntity<StockCountResponse> addLine(
            @Parameter(description = "Identifier of the count") @PathVariable Long id,
            @Valid @RequestBody StockCountLineRequest request) {
        return ResponseEntity.ok(stockCountService.addLine(id, request));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete a stock count",
            description = "Applies every counted line to the warehouse's stock as an ADJUSTMENT and "
                    + "closes the count. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Count completed and applied"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Count not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Count is not DRAFT, or nothing was counted",
                content = @Content)
    })
    public ResponseEntity<StockCountResponse> complete(
            @Parameter(description = "Identifier of the count") @PathVariable Long id) {
        return ResponseEntity.ok(stockCountService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a stock count",
            description = "Abandons a DRAFT count, leaving stock untouched. Requires the ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Count cancelled"),
        @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN", content = @Content),
        @ApiResponse(responseCode = "404", description = "Count not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Count is no longer DRAFT", content = @Content)
    })
    public ResponseEntity<StockCountResponse> cancel(
            @Parameter(description = "Identifier of the count") @PathVariable Long id) {
        return ResponseEntity.ok(stockCountService.cancel(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a stock count by id",
            description = "Returns the count with its lines and their variances.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Count found"),
        @ApiResponse(responseCode = "404", description = "Count not found", content = @Content)
    })
    public ResponseEntity<StockCountResponse> findById(
            @Parameter(description = "Identifier of the count") @PathVariable Long id) {
        return ResponseEntity.ok(stockCountService.findById(id));
    }

    /**
     * Lists counts, most recent first, optionally narrowed by warehouse and status.
     *
     * @param warehouseId identifier of a warehouse to filter by, or {@code null}
     * @param status      lifecycle status to filter by, or {@code null}
     * @return the matching counts
     */
    @GetMapping
    @Operation(summary = "List stock counts",
            description = "Returns counts most recent first. Filter by warehouse, by status, or by both.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Counts returned"),
        @ApiResponse(responseCode = "404", description = "Filtered warehouse not found", content = @Content)
    })
    public ResponseEntity<List<StockCountResponse>> find(
            @Parameter(description = "Only counts taken in this warehouse")
            @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "Only counts in this lifecycle status")
            @RequestParam(required = false) StockCountStatus status) {
        return ResponseEntity.ok(stockCountService.find(warehouseId, status));
    }

}
