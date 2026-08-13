package com.example.smartinventory.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.dto.StockLevelResponse;
import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockLevel;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.StockLevelRepository;

@ExtendWith(MockitoExtension.class)
class StockLevelServiceTest {

    private static final Product PRODUCT = Product.builder().id(1L).sku("SKU-1").name("Widget").build();
    private static final Warehouse WAREHOUSE = Warehouse.builder().id(7L).code("WH-1").name("Main Depot").build();

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @InjectMocks
    private StockLevelService stockLevelService;

    @Test
    void applyInCreatesLevelOnFirstUse() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        int delta = stockLevelService.apply(PRODUCT, WAREHOUSE, MovementType.IN, 4);

        assertThat(delta).isEqualTo(4);
        assertThat(captureSaved().getQuantity()).isEqualTo(4);
    }

    @Test
    void applyInAddsToExistingLevel() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.of(level(10)));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        int delta = stockLevelService.apply(PRODUCT, WAREHOUSE, MovementType.IN, 5);

        assertThat(delta).isEqualTo(5);
        assertThat(captureSaved().getQuantity()).isEqualTo(15);
    }

    @Test
    void applyOutSubtractsFromExistingLevel() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.of(level(10)));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        int delta = stockLevelService.apply(PRODUCT, WAREHOUSE, MovementType.OUT, 4);

        assertThat(delta).isEqualTo(-4);
        assertThat(captureSaved().getQuantity()).isEqualTo(6);
    }

    @Test
    void applyOutThrowsWhenWarehouseHoldsTooLittle() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.of(level(2)));

        assertThatThrownBy(() -> stockLevelService.apply(PRODUCT, WAREHOUSE, MovementType.OUT, 5))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("WH-1");
        verify(stockLevelRepository, never()).save(any(StockLevel.class));
    }

    @Test
    void applyAdjustmentSetsLevelAndReportsDifference() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.of(level(10)));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        int delta = stockLevelService.apply(PRODUCT, WAREHOUSE, MovementType.ADJUSTMENT, 6);

        assertThat(delta).isEqualTo(-4);
        assertThat(captureSaved().getQuantity()).isEqualTo(6);
    }

    @Test
    void applyTransferInAddsToTheDestinationLevel() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.of(level(10)));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        int delta = stockLevelService.apply(PRODUCT, WAREHOUSE, MovementType.TRANSFER_IN, 5);

        assertThat(delta).isEqualTo(5);
        assertThat(captureSaved().getQuantity()).isEqualTo(15);
    }

    @Test
    void applyTransferOutSubtractsFromTheSourceLevel() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.of(level(10)));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        int delta = stockLevelService.apply(PRODUCT, WAREHOUSE, MovementType.TRANSFER_OUT, 4);

        assertThat(delta).isEqualTo(-4);
        assertThat(captureSaved().getQuantity()).isEqualTo(6);
    }

    @Test
    void applyTransferOutThrowsWhenSourceHoldsTooLittle() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.of(level(2)));

        assertThatThrownBy(() -> stockLevelService.apply(PRODUCT, WAREHOUSE, MovementType.TRANSFER_OUT, 5))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("WH-1");
        verify(stockLevelRepository, never()).save(any(StockLevel.class));
    }

    @Test
    void quantityOnHandReportsWhatTheWarehouseHolds() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.of(level(12)));

        assertThat(stockLevelService.quantityOnHand(1L, 7L)).isEqualTo(12);
    }

    @Test
    void quantityOnHandIsZeroForANeverStockedProduct() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.empty());

        assertThat(stockLevelService.quantityOnHand(1L, 7L)).isZero();
    }

    @Test
    void findByWarehouseReturnsFlattenedLevels() {
        when(stockLevelRepository.findByWarehouseId(7L)).thenReturn(List.of(level(12)));

        List<StockLevelResponse> result = stockLevelService.findByWarehouse(7L);

        assertThat(result).singleElement().satisfies(entry -> {
            assertThat(entry.productId()).isEqualTo(1L);
            assertThat(entry.sku()).isEqualTo("SKU-1");
            assertThat(entry.warehouseCode()).isEqualTo("WH-1");
            assertThat(entry.quantity()).isEqualTo(12);
        });
        verify(warehouseService).findById(7L);
    }

    @Test
    void findByProductReturnsFlattenedLevels() {
        when(stockLevelRepository.findByProductId(1L)).thenReturn(List.of(level(3)));

        List<StockLevelResponse> result = stockLevelService.findByProduct(1L);

        assertThat(result).singleElement().satisfies(entry -> {
            assertThat(entry.warehouseId()).isEqualTo(7L);
            assertThat(entry.warehouseName()).isEqualTo("Main Depot");
            assertThat(entry.quantity()).isEqualTo(3);
        });
        verify(productService).findById(1L);
    }

    @Test
    void setReorderThresholdRecordsItOnTheExistingLevel() {
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.of(level(12)));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        StockLevelResponse result = stockLevelService.setReorderThreshold(1L, 7L, 5);

        assertThat(result.reorderThreshold()).isEqualTo(5);
        assertThat(result.quantity()).isEqualTo(12);
        assertThat(captureSaved().getReorderThreshold()).isEqualTo(5);
    }

    @Test
    void setReorderThresholdCreatesAnEmptyLevelForANeverStockedSite() {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(warehouseService.findById(7L)).thenReturn(WAREHOUSE);
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        StockLevelResponse result = stockLevelService.setReorderThreshold(1L, 7L, 5);

        assertThat(result.quantity()).isZero();
        assertThat(result.reorderThreshold()).isEqualTo(5);
    }

    @Test
    void setReorderThresholdClearsItWhenGivenNull() {
        StockLevel existing = level(12);
        existing.setReorderThreshold(5);
        when(stockLevelRepository.findByProductIdAndWarehouseId(1L, 7L)).thenReturn(Optional.of(existing));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(stockLevelService.setReorderThreshold(1L, 7L, null).reorderThreshold()).isNull();
    }

    private static StockLevel level(int quantity) {
        return StockLevel.builder().id(99L).product(PRODUCT).warehouse(WAREHOUSE).quantity(quantity).build();
    }

    private StockLevel captureSaved() {
        ArgumentCaptor<StockLevel> captor = ArgumentCaptor.forClass(StockLevel.class);
        verify(stockLevelRepository).save(captor.capture());
        return captor.getValue();
    }

}
