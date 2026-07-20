package com.example.smartinventory.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.smartinventory.dto.DashboardSummaryResponse;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.repository.CategoryRepository;
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.StockMovementRepository;
import com.example.smartinventory.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

/** Service computing aggregate counts and recent activity for the inventory dashboard. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    private final SupplierRepository supplierRepository;

    private final StockMovementRepository stockMovementRepository;

    private final ReportService reportService;

    private static final int MAX_RECENT_MOVEMENTS = 100;

    /**
     * Builds the dashboard summary: entity counts, low-stock count, and total stock value.
     *
     * @return the aggregate dashboard summary
     */
    public DashboardSummaryResponse summary() {
        return DashboardSummaryResponse.builder()
                .totalProducts(productRepository.count())
                .totalCategories(categoryRepository.count())
                .totalSuppliers(supplierRepository.count())
                .lowStockCount(productRepository.findLowStockProducts().size())
                .totalStockValue(reportService.totalStockValue())
                .build();
    }

    /**
     * Returns the most recent stock movements across all products. The requested limit is
     * clamped to the range {@code [1, 100]} to guard against invalid or excessive page sizes.
     *
     * @param limit maximum number of movements to return
     * @return recent movements, most recent first
     */
    public List<StockMovement> recentMovements(int limit) {
        int clampedLimit = Math.max(1, Math.min(limit, MAX_RECENT_MOVEMENTS));
        return stockMovementRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, clampedLimit));
    }

}
