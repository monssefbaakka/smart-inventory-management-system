package com.example.smartinventory.dto;

import java.math.BigDecimal;

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
public class DashboardSummaryResponse {

    private long totalProducts;

    private long totalCategories;

    private long totalSuppliers;

    private long lowStockCount;

    private BigDecimal totalStockValue;

}
