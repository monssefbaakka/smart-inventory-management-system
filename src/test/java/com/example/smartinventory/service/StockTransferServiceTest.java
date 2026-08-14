package com.example.smartinventory.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.smartinventory.dto.StockTransferResponse;
import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.exception.InvalidStockTransferException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockMovement;
import com.example.smartinventory.model.StockTransfer;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.StockMovementRepository;
import com.example.smartinventory.repository.StockTransferRepository;

@ExtendWith(MockitoExtension.class)
class StockTransferServiceTest {

    private static final Product PRODUCT = Product.builder().id(1L).sku("SKU-1").name("Widget").quantity(20).build();

    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private StockTransferRepository stockTransferRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private StockLevelService stockLevelService;

    @Mock
    private AutoReorderService autoReorderService;

    @InjectMocks
    private StockTransferService stockTransferService;

    @Test
    void transferMovesStockOutOfSourceAndIntoDestination() {
        Warehouse source = warehouse(1L, "WH-NORTH", true);
        Warehouse destination = warehouse(2L, "WH-SOUTH", true);
        stubLookups(source, destination);
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        StockTransferResponse result = stockTransferService.transfer(1L, 1L, 2L, 6, "rebalancing");

        verify(stockLevelService).apply(PRODUCT, source, MovementType.TRANSFER_OUT, 6);
        verify(stockLevelService).apply(PRODUCT, destination, MovementType.TRANSFER_IN, 6);
        assertThat(result.sourceWarehouseCode()).isEqualTo("WH-NORTH");
        assertThat(result.destinationWarehouseCode()).isEqualTo("WH-SOUTH");
        assertThat(result.sku()).isEqualTo("SKU-1");
        assertThat(result.quantity()).isEqualTo(6);
        assertThat(result.note()).isEqualTo("rebalancing");
    }

    @Test
    void transferLeavesProductOverallQuantityUnchanged() {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget").quantity(20).build();
        Warehouse source = warehouse(1L, "WH-NORTH", true);
        Warehouse destination = warehouse(2L, "WH-SOUTH", true);
        when(productService.findById(1L)).thenReturn(product);
        when(warehouseService.findById(1L)).thenReturn(source);
        when(warehouseService.findById(2L)).thenReturn(destination);
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        stockTransferService.transfer(1L, 1L, 2L, 6, null);

        assertThat(product.getQuantity()).isEqualTo(20);
    }

    @Test
    void transferWritesBothLegsIntoMovementHistory() {
        Warehouse source = warehouse(1L, "WH-NORTH", true);
        Warehouse destination = warehouse(2L, "WH-SOUTH", true);
        stubLookups(source, destination);
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        stockTransferService.transfer(1L, 1L, 2L, 6, "rebalancing");

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(2)).save(captor.capture());
        List<StockMovement> legs = captor.getAllValues();
        assertThat(legs.get(0).getType()).isEqualTo(MovementType.TRANSFER_OUT);
        assertThat(legs.get(0).getWarehouse()).isSameAs(source);
        assertThat(legs.get(1).getType()).isEqualTo(MovementType.TRANSFER_IN);
        assertThat(legs.get(1).getWarehouse()).isSameAs(destination);
        assertThat(legs).allSatisfy(leg -> {
            assertThat(leg.getProduct()).isSameAs(PRODUCT);
            assertThat(leg.getQuantity()).isEqualTo(6);
            assertThat(leg.getNote()).isEqualTo("rebalancing");
        });
    }

    @Test
    void transferMeasuresTheSourceAgainstItsOwnReorderPoint() {
        Warehouse source = warehouse(1L, "WH-NORTH", true);
        Warehouse destination = warehouse(2L, "WH-SOUTH", true);
        stubLookups(source, destination);
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        stockTransferService.transfer(1L, 1L, 2L, 6, null);

        verify(autoReorderService).evaluateRelocation(PRODUCT, source);
        verify(autoReorderService, never()).evaluateRelocation(PRODUCT, destination);
    }

    @Test
    void transferMeasuresTheSourceOnlyOnceTheStockHasActuallyMoved() {
        Warehouse source = warehouse(1L, "WH-NORTH", true);
        Warehouse destination = warehouse(2L, "WH-SOUTH", true);
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(warehouseService.findById(1L)).thenReturn(source);
        when(warehouseService.findById(2L)).thenReturn(destination);
        when(stockLevelService.apply(PRODUCT, source, MovementType.TRANSFER_OUT, 60))
                .thenThrow(new InsufficientStockException("only 2 in stock"));

        assertThatThrownBy(() -> stockTransferService.transfer(1L, 1L, 2L, 60, null))
                .isInstanceOf(InsufficientStockException.class);

        verifyNoInteractions(autoReorderService);
    }

    @Test
    void transferRejectsSameSourceAndDestination() {
        assertThatThrownBy(() -> stockTransferService.transfer(1L, 3L, 3L, 6, null))
                .isInstanceOf(InvalidStockTransferException.class)
                .hasMessageContaining("must differ");

        verifyNoInteractions(productService, warehouseService, stockLevelService, stockTransferRepository,
                autoReorderService);
    }

    @Test
    void transferRejectsInactiveDestination() {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(warehouseService.findById(1L)).thenReturn(warehouse(1L, "WH-NORTH", true));
        when(warehouseService.findById(2L)).thenReturn(warehouse(2L, "WH-CLOSED", false));

        assertThatThrownBy(() -> stockTransferService.transfer(1L, 1L, 2L, 6, null))
                .isInstanceOf(InvalidStockTransferException.class)
                .hasMessageContaining("WH-CLOSED");

        verifyNoInteractions(stockLevelService);
        verify(stockTransferRepository, never()).save(any(StockTransfer.class));
    }

    @Test
    void transferAllowsDrainingAnInactiveSource() {
        Warehouse source = warehouse(1L, "WH-CLOSED", false);
        Warehouse destination = warehouse(2L, "WH-SOUTH", true);
        stubLookups(source, destination);
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(inv -> inv.getArgument(0));

        StockTransferResponse result = stockTransferService.transfer(1L, 1L, 2L, 6, null);

        assertThat(result.sourceWarehouseCode()).isEqualTo("WH-CLOSED");
        verify(stockLevelService).apply(PRODUCT, source, MovementType.TRANSFER_OUT, 6);
    }

    @Test
    void transferStopsWhenSourceHoldsTooLittle() {
        Warehouse source = warehouse(1L, "WH-NORTH", true);
        Warehouse destination = warehouse(2L, "WH-SOUTH", true);
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(warehouseService.findById(1L)).thenReturn(source);
        when(warehouseService.findById(2L)).thenReturn(destination);
        when(stockLevelService.apply(PRODUCT, source, MovementType.TRANSFER_OUT, 60))
                .thenThrow(new InsufficientStockException("only 2 in stock"));

        assertThatThrownBy(() -> stockTransferService.transfer(1L, 1L, 2L, 60, null))
                .isInstanceOf(InsufficientStockException.class);

        verify(stockLevelService, never()).apply(PRODUCT, destination, MovementType.TRANSFER_IN, 60);
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
        verify(stockTransferRepository, never()).save(any(StockTransfer.class));
    }

    @Test
    void findByIdReturnsFlattenedTransfer() {
        when(stockTransferRepository.findById(5L)).thenReturn(Optional.of(transfer()));

        StockTransferResponse result = stockTransferService.findById(5L);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.productName()).isEqualTo("Widget");
        assertThat(result.sourceWarehouseId()).isEqualTo(1L);
        assertThat(result.destinationWarehouseId()).isEqualTo(2L);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(stockTransferRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockTransferService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findWithoutFiltersReturnsAPageOfTheWholeHistory() {
        when(stockTransferRepository.findAll(PAGEABLE)).thenReturn(page());

        Page<StockTransferResponse> result = stockTransferService.find(null, null, PAGEABLE);

        assertThat(result.getContent()).singleElement()
                .satisfies(entry -> assertThat(entry.quantity()).isEqualTo(6));
        assertThat(result.getTotalElements()).isEqualTo(1);
        verifyNoInteractions(productService, warehouseService);
    }

    @Test
    void findByProductFiltersOnProductAndChecksItExists() {
        when(stockTransferRepository.findByProductId(1L, PAGEABLE)).thenReturn(page());

        Page<StockTransferResponse> result = stockTransferService.find(1L, null, PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
        verify(productService).findById(1L);
        verifyNoInteractions(warehouseService);
    }

    @Test
    void findByWarehouseMatchesEitherSideOfTheMove() {
        when(stockTransferRepository.findBySourceWarehouseIdOrDestinationWarehouseId(2L, 2L, PAGEABLE))
                .thenReturn(page());

        Page<StockTransferResponse> result = stockTransferService.find(null, 2L, PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
        verify(warehouseService).findById(2L);
        verifyNoInteractions(productService);
    }

    @Test
    void findPrefersProductWhenBothFiltersAreGiven() {
        when(stockTransferRepository.findByProductId(1L, PAGEABLE)).thenReturn(page());

        stockTransferService.find(1L, 2L, PAGEABLE);

        verify(productService).findById(1L);
        verifyNoInteractions(warehouseService);
    }

    private void stubLookups(Warehouse source, Warehouse destination) {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(warehouseService.findById(source.getId())).thenReturn(source);
        when(warehouseService.findById(destination.getId())).thenReturn(destination);
    }

    private static Warehouse warehouse(Long id, String code, boolean active) {
        return Warehouse.builder().id(id).code(code).name("Depot " + code).active(active).build();
    }

    private static Page<StockTransfer> page() {
        return new PageImpl<>(List.of(transfer()), PAGEABLE, 1);
    }

    private static StockTransfer transfer() {
        return StockTransfer.builder()
                .id(5L)
                .product(PRODUCT)
                .sourceWarehouse(warehouse(1L, "WH-NORTH", true))
                .destinationWarehouse(warehouse(2L, "WH-SOUTH", true))
                .quantity(6)
                .note("rebalancing")
                .build();
    }

}
