package com.example.smartinventory.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aggregate counts and totals shown on the inventory dashboard. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Aggregate counts and totals for the dashboard")
public class DashboardSummaryResponse {

    @Schema(description = "Total number of products", example = "128")
    private long totalProducts;

    @Schema(description = "Total number of categories", example = "12")
    private long totalCategories;

    @Schema(description = "Total number of suppliers", example = "8")
    private long totalSuppliers;

    @Schema(description = "Number of products at or below their reorder threshold", example = "5")
    private long lowStockCount;

    @Schema(description = "Sum of price multiplied by quantity across all products", example = "10450.75")
    private BigDecimal totalStockValue;

}
