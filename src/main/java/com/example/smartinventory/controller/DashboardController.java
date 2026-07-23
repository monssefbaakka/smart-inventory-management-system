package com.example.smartinventory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smartinventory.dto.DashboardSummaryResponse;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** REST endpoints exposing the inventory dashboard: aggregate counts and recent activity. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregate inventory dashboard endpoints")
public class DashboardController {

    private static final String DEFAULT_RECENT_MOVEMENTS_LIMIT = "10";

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Dashboard summary",
            description = "Returns entity counts, low-stock count and total stock value.")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    public ResponseEntity<DashboardSummaryResponse> summary() {
        return ResponseEntity.ok(dashboardService.summary());
    }

    @GetMapping("/recent-movements")
    @Operation(summary = "Recent stock movements",
            description = "Returns the most recent stock movements across all products. "
                    + "The limit is clamped server-side to the range [1, 100].")
    @ApiResponse(responseCode = "200", description = "Recent movements returned")
    public ResponseEntity<List<StockMovement>> recentMovements(
            @Parameter(description = "Maximum number of movements to return (clamped to 1-100)")
            @RequestParam(defaultValue = DEFAULT_RECENT_MOVEMENTS_LIMIT) int limit) {
        return ResponseEntity.ok(dashboardService.recentMovements(limit));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Low-stock products",
            description = "Returns products at or below their reorder threshold.")
    @ApiResponse(responseCode = "200", description = "Low-stock products returned")
    public ResponseEntity<List<Product>> lowStockProducts() {
        return ResponseEntity.ok(dashboardService.lowStockProducts());
    }

}
