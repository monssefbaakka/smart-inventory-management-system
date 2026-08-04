package com.example.smartinventory.service;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.exception.InvalidBatchException;
import com.example.smartinventory.exception.InvalidStockTransferException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.ProductBatch;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.StockMovementRepository;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private StockLevelService stockLevelService;

    @Mock
    private StockEventNotificationService stockEventNotificationService;

    @Mock
    private AutoReorderService autoReorderService;

    @Mock
    private ProductBatchService productBatchService;

    @InjectMocks
    private StockMovementService stockMovementService;

    @Test
    void recordInIncreasesQuantity() {
        Product product = Product.builder().id(1L).quantity(5).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.record(1L, null, null, MovementType.IN, 3, "restock");

        assertThat(product.getQuantity()).isEqualTo(8);
        assertThat(result.getType()).isEqualTo(MovementType.IN);
        assertThat(result.getQuantity()).isEqualTo(3);
        verify(productRepository).save(product);
        verify(stockEventNotificationService).evaluate(product);
        verify(autoReorderService).evaluate(product);
    }

    @Test
    void recordOutDecreasesQuantity() {
        Product product = Product.builder().id(1L).quantity(5).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        stockMovementService.record(1L, null, null, MovementType.OUT, 2, null);

        assertThat(product.getQuantity()).isEqualTo(3);
    }

    @Test
    void recordOutThrowsWhenInsufficientStock() {
        Product product = Product.builder().id(1L).quantity(1).build();
        when(productService.findById(1L)).thenReturn(product);

        assertThatThrownBy(() -> stockMovementService.record(1L, null, null, MovementType.OUT, 5, null))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void recordAdjustmentSetsQuantity() {
        Product product = Product.builder().id(1L).quantity(5).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        stockMovementService.record(1L, null, null, MovementType.ADJUSTMENT, 42, "recount");

        assertThat(product.getQuantity()).isEqualTo(42);
    }

    @Test
    void recordWithWarehouseAppliesLevelAndMirrorsDeltaOntoProduct() {
        Product product = Product.builder().id(1L).quantity(5).build();
        Warehouse warehouse = Warehouse.builder().id(7L).code("WH-1").build();
        when(productService.findById(1L)).thenReturn(product);
        when(warehouseService.findById(7L)).thenReturn(warehouse);
        when(stockLevelService.apply(product, warehouse, MovementType.IN, 3)).thenReturn(3);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.record(1L, 7L, null, MovementType.IN, 3, "receipt");

        assertThat(product.getQuantity()).isEqualTo(8);
        assertThat(result.getWarehouse()).isSameAs(warehouse);
        verify(productRepository).save(product);
    }

    @Test
    void recordWithWarehouseAppliesNegativeDeltaForOut() {
        Product product = Product.builder().id(1L).quantity(5).build();
        Warehouse warehouse = Warehouse.builder().id(7L).code("WH-1").build();
        when(productService.findById(1L)).thenReturn(product);
        when(warehouseService.findById(7L)).thenReturn(warehouse);
        when(stockLevelService.apply(product, warehouse, MovementType.OUT, 2)).thenReturn(-2);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        stockMovementService.record(1L, 7L, null, MovementType.OUT, 2, null);

        assertThat(product.getQuantity()).isEqualTo(3);
    }

    @Test
    void recordWithWarehouseThrowsWhenDeltaWouldDriveTotalNegative() {
        Product product = Product.builder().id(1L).quantity(1).build();
        Warehouse warehouse = Warehouse.builder().id(7L).code("WH-1").build();
        when(productService.findById(1L)).thenReturn(product);
        when(warehouseService.findById(7L)).thenReturn(warehouse);
        when(stockLevelService.apply(product, warehouse, MovementType.ADJUSTMENT, 0)).thenReturn(-4);

        assertThatThrownBy(() -> stockMovementService.record(1L, 7L, null, MovementType.ADJUSTMENT, 0, null))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void recordWithoutWarehouseLeavesStockLevelsUntouched() {
        Product product = Product.builder().id(1L).quantity(5).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.record(1L, null, null, MovementType.IN, 3, null);

        assertThat(result.getWarehouse()).isNull();
        verifyNoInteractions(stockLevelService, warehouseService);
    }

    @Test
    void recordRejectsTransferLegs() {
        assertThatThrownBy(() -> stockMovementService.record(1L, 7L, null, MovementType.TRANSFER_IN, 3, null))
                .isInstanceOf(InvalidStockTransferException.class)
                .hasMessageContaining("TRANSFER_IN");

        verifyNoInteractions(productService, productRepository, stockLevelService, stockMovementRepository);
    }

    @Test
    void recordInNamingABatchReceivesIntoIt() {
        Product product = Product.builder().id(1L).quantity(5).build();
        ProductBatch batch = ProductBatch.builder().id(4L).product(product).lotCode("A-2291").quantity(2).build();
        when(productService.findById(1L)).thenReturn(product);
        when(productBatchService.findById(4L)).thenReturn(batch);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.record(1L, null, 4L, MovementType.IN, 3, null);

        assertThat(result.getBatch()).isSameAs(batch);
        verify(productBatchService).receive(batch, product, null, 3);
    }

    @Test
    void recordOutNamingABatchTakesFromIt() {
        Product product = Product.builder().id(1L).quantity(5).build();
        ProductBatch batch = ProductBatch.builder().id(4L).product(product).lotCode("A-2291").quantity(5).build();
        when(productService.findById(1L)).thenReturn(product);
        when(productBatchService.findById(4L)).thenReturn(batch);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        stockMovementService.record(1L, null, 4L, MovementType.OUT, 2, null);

        verify(productBatchService).consume(batch, product, null, 2);
        verify(productBatchService, never()).consumeEarliestExpiryFirst(any(), any(), anyInt());
    }

    @Test
    void recordOutNamingNoBatchIsAllocatedAcrossTheProductBatches() {
        Product product = Product.builder().id(1L).quantity(5).build();
        when(productService.findById(1L)).thenReturn(product);
        when(productBatchService.hasStockedBatches(1L)).thenReturn(true);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.record(1L, null, null, MovementType.OUT, 2, null);

        assertThat(result.getBatch()).isNull();
        verify(productBatchService).consumeEarliestExpiryFirst(product, null, 2);
    }

    @Test
    void recordOutLeavesAProductWithoutBatchesAlone() {
        Product product = Product.builder().id(1L).quantity(5).build();
        when(productService.findById(1L)).thenReturn(product);
        when(productBatchService.hasStockedBatches(1L)).thenReturn(false);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        stockMovementService.record(1L, null, null, MovementType.OUT, 2, null);

        assertThat(product.getQuantity()).isEqualTo(3);
        verify(productBatchService, never()).consumeEarliestExpiryFirst(any(), any(), anyInt());
    }

    @Test
    void recordRejectsAnAdjustmentNamingABatch() {
        assertThatThrownBy(() -> stockMovementService.record(1L, null, 4L, MovementType.ADJUSTMENT, 9, null))
                .isInstanceOf(InvalidBatchException.class)
                .hasMessageContaining("ADJUSTMENT");

        verifyNoInteractions(productService, productBatchService, stockMovementRepository);
    }

    @Test
    void recordAdjustmentLeavesTheBatchesUntouched() {
        Product product = Product.builder().id(1L).quantity(5).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        stockMovementService.record(1L, null, null, MovementType.ADJUSTMENT, 42, "recount");

        verifyNoInteractions(productBatchService);
    }

    @Test
    void receivingAtAStatedCostRollsTheWeightedAverage() {
        Product product = Product.builder().id(1L).quantity(200).averageCost(new BigDecimal("4.0000")).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.record(1L, null, null, MovementType.IN, 100, null,
                new BigDecimal("7.00"));

        assertThat(product.getAverageCost()).isEqualByComparingTo("5.0000");
        assertThat(result.getUnitCost()).isEqualByComparingTo("7.00");
        assertThat(result.getTotalCost()).isEqualByComparingTo("700.00");
    }

    @Test
    void theFirstReceiptOfAProductTakesItsCostOutright() {
        Product product = Product.builder().id(1L).quantity(0).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        stockMovementService.record(1L, null, null, MovementType.IN, 10, null, new BigDecimal("3.25"));

        assertThat(product.getAverageCost()).isEqualByComparingTo("3.2500");
    }

    @Test
    void receivingWithoutACostLeavesTheAverageWhereItWas() {
        Product product = Product.builder().id(1L).quantity(10).averageCost(new BigDecimal("4.0000")).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.record(1L, null, null, MovementType.IN, 5, null, null);

        assertThat(product.getAverageCost()).isEqualByComparingTo("4.0000");
        assertThat(result.getUnitCost()).isEqualByComparingTo("4.0000");
        assertThat(result.getTotalCost()).isEqualByComparingTo("20.0000");
    }

    @Test
    void anOutwardMovementIsValuedAtTheAverageAndDoesNotDisturbIt() {
        Product product = Product.builder().id(1L).quantity(10).averageCost(new BigDecimal("4.5000")).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.record(1L, null, null, MovementType.OUT, 4, null,
                new BigDecimal("99.00"));

        assertThat(product.getAverageCost()).isEqualByComparingTo("4.5000");
        assertThat(result.getUnitCost()).isEqualByComparingTo("4.5000");
        assertThat(result.getTotalCost()).isEqualByComparingTo("18.0000");
    }

    @Test
    void anAdjustmentIsValuedAtTheAverageAndIgnoresAStatedCost() {
        Product product = Product.builder().id(1L).quantity(10).averageCost(new BigDecimal("2.0000")).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovement result = stockMovementService.record(1L, null, null, MovementType.ADJUSTMENT, 7, "recount",
                new BigDecimal("50.00"));

        assertThat(product.getAverageCost()).isEqualByComparingTo("2.0000");
        assertThat(result.getUnitCost()).isEqualByComparingTo("2.0000");
        assertThat(result.getTotalCost()).isEqualByComparingTo("14.0000");
    }

    @Test
    void anAverageThatDoesNotDivideEvenlyIsKeptToFourPlaces() {
        Product product = Product.builder().id(1L).quantity(1).averageCost(new BigDecimal("1.0000")).build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        stockMovementService.record(1L, null, null, MovementType.IN, 2, null, new BigDecimal("2.00"));

        assertThat(product.getAverageCost()).isEqualByComparingTo("1.6667");
    }

    @Test
    void findByProductReturnsAPageOfHistoryAndChecksTheProductExists() {
        Pageable pageable = PageRequest.of(0, 20);
        Product product = Product.builder().id(1L).build();
        StockMovement movement = StockMovement.builder().id(1L).product(product).type(MovementType.IN).quantity(3)
                .build();
        when(productService.findById(1L)).thenReturn(product);
        when(stockMovementRepository.findByProductId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(movement), pageable, 1));

        Page<StockMovement> result = stockMovementService.findByProduct(1L, pageable);

        assertThat(result.getContent()).containsExactly(movement);
        verify(productService).findById(1L);
    }

}
