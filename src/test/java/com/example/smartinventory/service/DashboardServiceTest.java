package com.example.smartinventory.service;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.example.smartinventory.dto.DashboardSummaryResponse;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.repository.CategoryRepository;
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.StockMovementRepository;
import com.example.smartinventory.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void summaryAggregatesCountsAndStockValue() {
        Product lowStock = Product.builder().id(1L).quantity(1).reorderThreshold(5).build();
        when(productRepository.count()).thenReturn(7L);
        when(categoryRepository.count()).thenReturn(3L);
        when(supplierRepository.count()).thenReturn(2L);
        when(productRepository.findLowStockProducts()).thenReturn(List.of(lowStock));
        when(reportService.totalStockValue()).thenReturn(new BigDecimal("199.99"));

        DashboardSummaryResponse result = dashboardService.summary();

        assertThat(result.getTotalProducts()).isEqualTo(7L);
        assertThat(result.getTotalCategories()).isEqualTo(3L);
        assertThat(result.getTotalSuppliers()).isEqualTo(2L);
        assertThat(result.getLowStockCount()).isEqualTo(1L);
        assertThat(result.getTotalStockValue()).isEqualByComparingTo("199.99");
    }

    @Test
    void summaryReportsZeroLowStockWhenNoneBelowThreshold() {
        when(productRepository.count()).thenReturn(0L);
        when(categoryRepository.count()).thenReturn(0L);
        when(supplierRepository.count()).thenReturn(0L);
        when(productRepository.findLowStockProducts()).thenReturn(List.of());
        when(reportService.totalStockValue()).thenReturn(BigDecimal.ZERO);

        DashboardSummaryResponse result = dashboardService.summary();

        assertThat(result.getLowStockCount()).isZero();
    }

    @Test
    void recentMovementsDelegatesToRepositoryWithLimit() {
        Product product = Product.builder().id(1L).build();
        StockMovement movement = StockMovement.builder().id(1L).product(product).type(MovementType.IN).quantity(5)
                .build();
        when(stockMovementRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of(movement));

        List<StockMovement> result = dashboardService.recentMovements(10);

        assertThat(result).containsExactly(movement);
    }

    @Test
    void recentMovementsClampsNonPositiveLimitToOne() {
        when(stockMovementRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of());

        dashboardService.recentMovements(0);

        verify(stockMovementRepository).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1));
    }

    @Test
    void recentMovementsClampsNegativeLimitToOne() {
        when(stockMovementRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of());

        dashboardService.recentMovements(-5);

        verify(stockMovementRepository).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1));
    }

    @Test
    void recentMovementsClampsExcessiveLimitToMaximum() {
        when(stockMovementRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(List.of());

        dashboardService.recentMovements(10_000);

        verify(stockMovementRepository).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100));
    }

    @Test
    void lowStockProductsDelegatesToRepository() {
        Product product = Product.builder().id(1L).quantity(1).reorderThreshold(5).build();
        when(productRepository.findLowStockProducts()).thenReturn(List.of(product));

        List<Product> result = dashboardService.lowStockProducts();

        assertThat(result).containsExactly(product);
    }

    @Test
    void summaryCountsAllLowStockProducts() {
        Product first = Product.builder().id(1L).quantity(1).reorderThreshold(5).build();
        Product second = Product.builder().id(2L).quantity(2).reorderThreshold(10).build();
        Product third = Product.builder().id(3L).quantity(0).reorderThreshold(3).build();
        when(productRepository.count()).thenReturn(3L);
        when(categoryRepository.count()).thenReturn(1L);
        when(supplierRepository.count()).thenReturn(1L);
        when(productRepository.findLowStockProducts()).thenReturn(List.of(first, second, third));
        when(reportService.totalStockValue()).thenReturn(BigDecimal.ZERO);

        DashboardSummaryResponse result = dashboardService.summary();

        assertThat(result.getLowStockCount()).isEqualTo(3L);
    }

}
