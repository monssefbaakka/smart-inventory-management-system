package com.example.smartinventory.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.dto.ProductBatchRequest;
import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.exception.InvalidBatchException;
import com.example.smartinventory.exception.InvalidBatchStateException;
import com.example.smartinventory.exception.InvalidQueryParameterException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.ProductBatch;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.ProductBatchRepository;

@ExtendWith(MockitoExtension.class)
class ProductBatchServiceTest {

    private static final Product PRODUCT = Product.builder().id(1L).sku("SKU-1").name("Yoghurt").build();

    @Mock
    private ProductBatchRepository productBatchRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @InjectMocks
    private ProductBatchService productBatchService;

    @Test
    void createDeclaresAnEmptyLotAgainstTheProduct() {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(productBatchRepository.existsByProductIdAndLotCode(1L, "A-2291")).thenReturn(false);
        when(productBatchRepository.save(any(ProductBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductBatch batch = productBatchService.create(1L,
                new ProductBatchRequest("A-2291", LocalDate.of(2026, 12, 31), null));

        assertThat(batch.getProduct()).isSameAs(PRODUCT);
        assertThat(batch.getLotCode()).isEqualTo("A-2291");
        assertThat(batch.getExpiryDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(batch.getQuantity()).isZero();
        assertThat(batch.getWarehouse()).isNull();
    }

    @Test
    void createResolvesTheNamedWarehouse() {
        Warehouse warehouse = Warehouse.builder().id(7L).code("WH-1").build();
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(productBatchRepository.existsByProductIdAndLotCode(1L, "A-2291")).thenReturn(false);
        when(warehouseService.findById(7L)).thenReturn(warehouse);
        when(productBatchRepository.save(any(ProductBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductBatch batch = productBatchService.create(1L, new ProductBatchRequest("A-2291", null, 7L));

        assertThat(batch.getWarehouse()).isSameAs(warehouse);
    }

    @Test
    void createRejectsALotCodeTheProductAlreadyCarries() {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(productBatchRepository.existsByProductIdAndLotCode(1L, "A-2291")).thenReturn(true);

        assertThatThrownBy(() -> productBatchService.create(1L, new ProductBatchRequest("A-2291", null, null)))
                .isInstanceOf(InvalidBatchStateException.class)
                .hasMessageContaining("A-2291");

        verify(productBatchRepository, never()).save(any(ProductBatch.class));
    }

    @Test
    void findByIdReportsAMissingBatch() {
        when(productBatchRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productBatchService.findById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9");
    }

    @Test
    void findByProductChecksTheProductExists() {
        ProductBatch batch = batch("A-2291", null, 5);
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(productBatchRepository.findByProduct(1L)).thenReturn(List.of(batch));

        assertThat(productBatchService.findByProduct(1L)).containsExactly(batch);
        verify(productService).findById(1L);
    }

    @Test
    void deleteRemovesAnEmptyLot() {
        ProductBatch batch = batch("A-2291", null, 0);
        when(productBatchRepository.findById(4L)).thenReturn(Optional.of(batch));

        productBatchService.delete(4L);

        verify(productBatchRepository).delete(batch);
    }

    @Test
    void deleteRefusesALotThatStillHoldsStock() {
        ProductBatch batch = batch("A-2291", null, 12);
        when(productBatchRepository.findById(4L)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> productBatchService.delete(4L))
                .isInstanceOf(InvalidBatchStateException.class)
                .hasMessageContaining("12");

        verify(productBatchRepository, never()).delete(any(ProductBatch.class));
    }

    @Test
    void findExpiringWithinLooksForwardFromToday() {
        ProductBatch batch = batch("A-2291", LocalDate.now().plusDays(3), 5);
        when(productBatchRepository.findExpiringBetween(eq(LocalDate.now()), eq(LocalDate.now().plusDays(30))))
                .thenReturn(List.of(batch));

        assertThat(productBatchService.findExpiringWithin(30)).containsExactly(batch);
    }

    @Test
    void findExpiringWithinRejectsANegativeWindow() {
        assertThatThrownBy(() -> productBatchService.findExpiringWithin(-1))
                .isInstanceOf(InvalidQueryParameterException.class)
                .hasMessageContaining("days");

        verifyNoInteractions(productBatchRepository);
    }

    @Test
    void findExpiredJudgesTheLotsAgainstToday() {
        ProductBatch batch = batch("A-2291", LocalDate.now().minusDays(1), 5);
        when(productBatchRepository.findExpiredOn(LocalDate.now())).thenReturn(List.of(batch));

        assertThat(productBatchService.findExpired()).containsExactly(batch);
    }

    @Test
    void receiveAddsToTheLot() {
        ProductBatch batch = batch("A-2291", null, 5);

        productBatchService.receive(batch, PRODUCT, null, 7);

        assertThat(batch.getQuantity()).isEqualTo(12);
        verify(productBatchRepository).save(batch);
    }

    @Test
    void consumeTakesFromTheLot() {
        ProductBatch batch = batch("A-2291", null, 5);

        productBatchService.consume(batch, PRODUCT, null, 3);

        assertThat(batch.getQuantity()).isEqualTo(2);
    }

    @Test
    void consumeRefusesToTakeMoreThanTheLotHolds() {
        ProductBatch batch = batch("A-2291", null, 2);

        assertThatThrownBy(() -> productBatchService.consume(batch, PRODUCT, null, 3))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("A-2291");

        assertThat(batch.getQuantity()).isEqualTo(2);
    }

    @Test
    void aLotOfAnotherProductCannotTakePartInTheMovement() {
        ProductBatch batch = batch("A-2291", null, 5);
        Product other = Product.builder().id(2L).build();

        assertThatThrownBy(() -> productBatchService.receive(batch, other, null, 1))
                .isInstanceOf(InvalidBatchException.class)
                .hasMessageContaining("product 2");
    }

    @Test
    void aLotHeldElsewhereCannotTakePartInTheMovement() {
        ProductBatch batch = batch("A-2291", null, 5);
        batch.setWarehouse(Warehouse.builder().id(7L).code("WH-1").build());

        assertThatThrownBy(() -> productBatchService.receive(batch, PRODUCT,
                Warehouse.builder().id(8L).code("WH-2").build(), 1))
                .isInstanceOf(InvalidBatchException.class);
    }

    @Test
    void aLotWithoutAWarehouseOnlyTakesPartInMovementsWithoutOne() {
        ProductBatch batch = batch("A-2291", null, 5);

        assertThatThrownBy(() -> productBatchService.receive(batch, PRODUCT,
                Warehouse.builder().id(7L).code("WH-1").build(), 1))
                .isInstanceOf(InvalidBatchException.class);
    }

    @Test
    void allocationDrawsOnTheEarliestExpiryFirstAndSpillsIntoTheNextLot() {
        ProductBatch soonest = batch("A", LocalDate.now().plusDays(1), 4);
        ProductBatch later = batch("B", LocalDate.now().plusDays(9), 10);
        when(productBatchRepository.findAllocatable(1L)).thenReturn(List.of(soonest, later));

        List<ProductBatch> drawnOn = productBatchService.consumeEarliestExpiryFirst(PRODUCT, null, 6);

        assertThat(drawnOn).containsExactly(soonest, later);
        assertThat(soonest.getQuantity()).isZero();
        assertThat(later.getQuantity()).isEqualTo(8);
    }

    @Test
    void allocationStopsOnceTheMovementIsSatisfied() {
        ProductBatch soonest = batch("A", LocalDate.now().plusDays(1), 10);
        ProductBatch later = batch("B", LocalDate.now().plusDays(9), 10);
        when(productBatchRepository.findAllocatable(1L)).thenReturn(List.of(soonest, later));

        List<ProductBatch> drawnOn = productBatchService.consumeEarliestExpiryFirst(PRODUCT, null, 4);

        assertThat(drawnOn).containsExactly(soonest);
        assertThat(later.getQuantity()).isEqualTo(10);
    }

    @Test
    void allocationLeavesEveryLotAloneWhenTheyTogetherHoldTooLittle() {
        ProductBatch soonest = batch("A", LocalDate.now().plusDays(1), 4);
        ProductBatch later = batch("B", LocalDate.now().plusDays(9), 3);
        when(productBatchRepository.findAllocatable(1L)).thenReturn(List.of(soonest, later));

        assertThatThrownBy(() -> productBatchService.consumeEarliestExpiryFirst(PRODUCT, null, 10))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("only 7");

        assertThat(soonest.getQuantity()).isEqualTo(4);
        assertThat(later.getQuantity()).isEqualTo(3);
        verify(productBatchRepository, never()).save(any(ProductBatch.class));
    }

    @Test
    void allocationFromAWarehouseOnlyDrawsOnTheStockHeldThere() {
        Warehouse warehouse = Warehouse.builder().id(7L).code("WH-1").build();
        ProductBatch here = batch("A", LocalDate.now().plusDays(1), 5);
        here.setWarehouse(warehouse);
        when(productBatchRepository.findAllocatableInWarehouse(1L, 7L)).thenReturn(List.of(here));

        productBatchService.consumeEarliestExpiryFirst(PRODUCT, warehouse, 5);

        assertThat(here.getQuantity()).isZero();
        verify(productBatchRepository, never()).findAllocatable(1L);
    }

    @Test
    void hasStockedBatchesReportsWhetherAnythingIsAllocatable() {
        when(productBatchRepository.findAllocatable(1L)).thenReturn(List.of());

        assertThat(productBatchService.hasStockedBatches(1L)).isFalse();
    }

    private ProductBatch batch(String lotCode, LocalDate expiryDate, int quantity) {
        return ProductBatch.builder()
                .id(4L)
                .product(PRODUCT)
                .lotCode(lotCode)
                .expiryDate(expiryDate)
                .quantity(quantity)
                .build();
    }

}
