package com.example.smartinventory.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.dto.StockCountLineRequest;
import com.example.smartinventory.dto.StockCountRequest;
import com.example.smartinventory.dto.StockCountResponse;
import com.example.smartinventory.exception.InvalidStockCountStateException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockCount;
import com.example.smartinventory.model.StockCountLine;
import com.example.smartinventory.model.StockCountStatus;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.StockCountRepository;

@ExtendWith(MockitoExtension.class)
class StockCountServiceTest {

    private static final Warehouse WAREHOUSE =
            Warehouse.builder().id(7L).code("WH-1").name("Main Depot").build();
    private static final Product PRODUCT = Product.builder().id(1L).sku("SKU-1").name("Widget").build();

    @Mock
    private StockCountRepository stockCountRepository;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private ProductService productService;

    @Mock
    private StockLevelService stockLevelService;

    @Mock
    private StockMovementService stockMovementService;

    @InjectMocks
    private StockCountService stockCountService;

    @Test
    void openCreatesDraftCountAgainstTheWarehouse() {
        when(warehouseService.findById(7L)).thenReturn(WAREHOUSE);
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(inv -> inv.getArgument(0));

        StockCountResponse result = stockCountService.open(new StockCountRequest(7L, "quarterly"));

        assertThat(result.status()).isEqualTo(StockCountStatus.DRAFT);
        assertThat(result.warehouseCode()).isEqualTo("WH-1");
        assertThat(result.note()).isEqualTo("quarterly");
        assertThat(result.lines()).isEmpty();
        verifyNoInteractions(stockMovementService);
    }

    @Test
    void addLineSnapshotsWhatTheWarehouseWasBelievedToHold() {
        StockCount count = draftCount();
        stubLine(count);
        when(stockLevelService.quantityOnHand(1L, 7L)).thenReturn(42);

        StockCountResponse result = stockCountService.addLine(3L, new StockCountLineRequest(1L, 38));

        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.sku()).isEqualTo("SKU-1");
            assertThat(line.countedQuantity()).isEqualTo(38);
            assertThat(line.expectedQuantity()).isEqualTo(42);
            assertThat(line.variance()).isEqualTo(-4);
        });
        assertThat(result.totalVariance()).isEqualTo(-4);
    }

    @Test
    void addLineTreatsAnUnstockedProductAsZeroExpected() {
        StockCount count = draftCount();
        stubLine(count);
        when(stockLevelService.quantityOnHand(1L, 7L)).thenReturn(0);

        StockCountResponse result = stockCountService.addLine(3L, new StockCountLineRequest(1L, 5));

        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.expectedQuantity()).isZero();
            assertThat(line.variance()).isEqualTo(5);
        });
    }

    @Test
    void addLineReplacesTheEarlierLineForTheSameProduct() {
        StockCount count = draftCount();
        count.addLine(StockCountLine.builder().id(9L).product(PRODUCT).countedQuantity(38).expectedQuantity(42)
                .build());
        stubLine(count);
        when(stockLevelService.quantityOnHand(1L, 7L)).thenReturn(42);

        StockCountResponse result = stockCountService.addLine(3L, new StockCountLineRequest(1L, 40));

        assertThat(result.lines()).singleElement().satisfies(line -> {
            assertThat(line.id()).isEqualTo(9L);
            assertThat(line.countedQuantity()).isEqualTo(40);
            assertThat(line.variance()).isEqualTo(-2);
        });
    }

    @Test
    void addLineRejectedOnceTheCountIsNoLongerDraft() {
        when(stockCountRepository.findById(3L)).thenReturn(Optional.of(countWithStatus(StockCountStatus.COMPLETED)));

        assertThatThrownBy(() -> stockCountService.addLine(3L, new StockCountLineRequest(1L, 40)))
                .isInstanceOf(InvalidStockCountStateException.class)
                .hasMessageContaining("COMPLETED");

        verifyNoInteractions(productService, stockLevelService);
        verify(stockCountRepository, never()).save(any(StockCount.class));
    }

    @Test
    void completeAppliesOneAdjustmentPerCountedLine() {
        StockCount count = draftCount();
        count.addLine(line(PRODUCT, 38, 42));
        Product other = Product.builder().id(2L).sku("SKU-2").name("Gadget").build();
        count.addLine(line(other, 7, 5));
        when(stockCountRepository.findById(3L)).thenReturn(Optional.of(count));
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(inv -> inv.getArgument(0));

        StockCountResponse result = stockCountService.complete(3L);

        verify(stockMovementService).record(1L, 7L, MovementType.ADJUSTMENT, 38, "Stock count #3");
        verify(stockMovementService).record(2L, 7L, MovementType.ADJUSTMENT, 7, "Stock count #3");
        assertThat(result.status()).isEqualTo(StockCountStatus.COMPLETED);
        assertThat(result.completedAt()).isNotNull();
        assertThat(result.totalVariance()).isEqualTo(-2);
    }

    @Test
    void completeRejectedWhenNothingWasCounted() {
        when(stockCountRepository.findById(3L)).thenReturn(Optional.of(draftCount()));

        assertThatThrownBy(() -> stockCountService.complete(3L))
                .isInstanceOf(InvalidStockCountStateException.class)
                .hasMessageContaining("nothing was counted");

        verifyNoInteractions(stockMovementService);
        verify(stockCountRepository, never()).save(any(StockCount.class));
    }

    @Test
    void completeRejectedWhenTheCountIsAlreadyCancelled() {
        when(stockCountRepository.findById(3L)).thenReturn(Optional.of(countWithStatus(StockCountStatus.CANCELLED)));

        assertThatThrownBy(() -> stockCountService.complete(3L))
                .isInstanceOf(InvalidStockCountStateException.class)
                .hasMessageContaining("CANCELLED");

        verify(stockMovementService, never()).record(anyLong(), anyLong(), any(MovementType.class), any(),
                anyString());
    }

    @Test
    void cancelAbandonsTheCountWithoutTouchingStock() {
        when(stockCountRepository.findById(3L)).thenReturn(Optional.of(draftCount()));
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(inv -> inv.getArgument(0));

        StockCountResponse result = stockCountService.cancel(3L);

        assertThat(result.status()).isEqualTo(StockCountStatus.CANCELLED);
        assertThat(result.completedAt()).isNull();
        verifyNoInteractions(stockMovementService, stockLevelService);
    }

    @Test
    void cancelRejectedOnceTheCountIsCompleted() {
        when(stockCountRepository.findById(3L)).thenReturn(Optional.of(countWithStatus(StockCountStatus.COMPLETED)));

        assertThatThrownBy(() -> stockCountService.cancel(3L))
                .isInstanceOf(InvalidStockCountStateException.class);

        verify(stockCountRepository, never()).save(any(StockCount.class));
    }

    @Test
    void findByIdReturnsTheCountWithItsLines() {
        StockCount count = draftCount();
        count.addLine(line(PRODUCT, 38, 42));
        when(stockCountRepository.findById(3L)).thenReturn(Optional.of(count));

        StockCountResponse result = stockCountService.findById(3L);

        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.lines()).hasSize(1);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(stockCountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockCountService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findWithoutFiltersReturnsEveryCount() {
        when(stockCountRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(draftCount()));

        List<StockCountResponse> result = stockCountService.find(null, null);

        assertThat(result).hasSize(1);
        verifyNoInteractions(warehouseService);
    }

    @Test
    void findByWarehouseChecksTheWarehouseExists() {
        when(stockCountRepository.findByWarehouseIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(draftCount()));

        List<StockCountResponse> result = stockCountService.find(7L, null);

        assertThat(result).hasSize(1);
        verify(warehouseService).findById(7L);
    }

    @Test
    void findByStatusFiltersOnStatusAlone() {
        when(stockCountRepository.findByStatusOrderByCreatedAtDesc(StockCountStatus.DRAFT))
                .thenReturn(List.of(draftCount()));

        List<StockCountResponse> result = stockCountService.find(null, StockCountStatus.DRAFT);

        assertThat(result).hasSize(1);
        verifyNoInteractions(warehouseService);
    }

    @Test
    void findByWarehouseAndStatusCombinesBothFilters() {
        when(stockCountRepository.findByWarehouseIdAndStatusOrderByCreatedAtDesc(7L, StockCountStatus.COMPLETED))
                .thenReturn(List.of(countWithStatus(StockCountStatus.COMPLETED)));

        List<StockCountResponse> result = stockCountService.find(7L, StockCountStatus.COMPLETED);

        assertThat(result).singleElement()
                .satisfies(count -> assertThat(count.status()).isEqualTo(StockCountStatus.COMPLETED));
        verify(warehouseService).findById(7L);
    }

    private void stubLine(StockCount count) {
        when(stockCountRepository.findById(3L)).thenReturn(Optional.of(count));
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(stockCountRepository.save(any(StockCount.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static StockCount draftCount() {
        return StockCount.builder().id(3L).warehouse(WAREHOUSE).status(StockCountStatus.DRAFT).build();
    }

    private static StockCount countWithStatus(StockCountStatus status) {
        return StockCount.builder().id(3L).warehouse(WAREHOUSE).status(status).build();
    }

    private static StockCountLine line(Product product, int counted, int expected) {
        return StockCountLine.builder().id(9L).product(product).countedQuantity(counted)
                .expectedQuantity(expected).build();
    }

}
