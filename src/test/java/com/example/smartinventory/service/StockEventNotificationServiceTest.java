package com.example.smartinventory.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.StockLevel;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.notification.StockEventNotification;
import com.example.smartinventory.notification.StockEventNotifier;
import com.example.smartinventory.notification.StockEventType;
import com.example.smartinventory.repository.StockLevelRepository;

@ExtendWith(MockitoExtension.class)
class StockEventNotificationServiceTest {

    @Mock
    private StockEventNotifier notifierA;

    @Mock
    private StockEventNotifier notifierB;

    @Mock
    private StockLevelRepository stockLevelRepository;

    private StockEventNotificationService service(StockEventNotifier... notifiers) {
        return new StockEventNotificationService(List.of(notifiers), stockLevelRepository);
    }

    private Warehouse warehouse() {
        return Warehouse.builder().id(7L).code("WH-NORTH").build();
    }

    /** Makes the north warehouse hold {@code quantity} units of the product against its own threshold. */
    private void siteHolds(Product product, Warehouse warehouse, Integer quantity, Integer threshold) {
        lenient().when(stockLevelRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()))
                .thenReturn(Optional.of(StockLevel.builder()
                        .product(product)
                        .warehouse(warehouse)
                        .quantity(quantity)
                        .reorderThreshold(threshold)
                        .build()));
    }

    private StockEventNotification captureFrom(StockEventNotifier notifier) {
        ArgumentCaptor<StockEventNotification> captor = ArgumentCaptor.forClass(StockEventNotification.class);
        verify(notifier).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void dispatchesLowStockToAllChannels() {
        Product product = Product.builder().id(1L).sku("SKU-1").name("Widget")
                .quantity(5).reorderThreshold(10).build();

        service(notifierA, notifierB).evaluate(product);

        verify(notifierB).send(any(StockEventNotification.class));
        StockEventNotification sent = captureFrom(notifierA);
        assertThat(sent.eventType()).isEqualTo(StockEventType.LOW_STOCK);
        assertThat(sent.productId()).isEqualTo(1L);
        assertThat(sent.sku()).isEqualTo("SKU-1");
        assertThat(sent.warehouseId()).isNull();
        assertThat(sent.warehouseCode()).isNull();
        assertThat(sent.occurredAt()).isNotNull();
    }

    @Test
    void dispatchesOutOfStockWhenQuantityZero() {
        Product product = Product.builder().id(2L).sku("SKU-2").name("Gadget")
                .quantity(0).reorderThreshold(10).build();

        service(notifierA).evaluate(product);

        assertThat(captureFrom(notifierA).eventType()).isEqualTo(StockEventType.OUT_OF_STOCK);
    }

    @Test
    void skipsNotificationWhenAboveThreshold() {
        Product product = Product.builder().id(3L).quantity(50).reorderThreshold(10).build();

        service(notifierA).evaluate(product);

        verify(notifierA, never()).send(any());
    }

    @Test
    void skipsNotificationWhenQuantityOrThresholdMissing() {
        Product noQuantity = Product.builder().id(4L).quantity(null).reorderThreshold(10).build();
        Product noThreshold = Product.builder().id(5L).quantity(5).reorderThreshold(null).build();

        StockEventNotificationService service = service(notifierA);
        service.evaluate(noQuantity);
        service.evaluate(noThreshold);

        verify(notifierA, never()).send(any());
    }

    @Test
    void continuesDispatchWhenAChannelThrows() {
        Product product = Product.builder().id(6L).sku("SKU-6").name("Thing")
                .quantity(1).reorderThreshold(10).build();
        doThrow(new RuntimeException("boom")).when(notifierA).send(any());

        service(notifierA, notifierB).evaluate(product);

        verify(notifierB).send(any(StockEventNotification.class));
    }

    @Test
    void alertsTheSiteThatReachedItsOwnReorderPoint() {
        Product product = Product.builder().id(10L).sku("SKU-10").name("Widget")
                .quantity(40).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        siteHolds(product, warehouse, 2, 5);

        service(notifierA).evaluate(product, warehouse);

        StockEventNotification sent = captureFrom(notifierA);
        assertThat(sent.eventType()).isEqualTo(StockEventType.LOW_STOCK);
        assertThat(sent.warehouseId()).isEqualTo(7L);
        assertThat(sent.warehouseCode()).isEqualTo("WH-NORTH");
        assertThat(sent.quantity()).isEqualTo(2);
        assertThat(sent.reorderThreshold()).isEqualTo(5);
    }

    @Test
    void alertsAnEmptySiteAsOutOfStock() {
        Product product = Product.builder().id(11L).sku("SKU-11").name("Widget")
                .quantity(40).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        siteHolds(product, warehouse, 0, 5);

        service(notifierA).evaluate(product, warehouse);

        assertThat(captureFrom(notifierA).eventType()).isEqualTo(StockEventType.OUT_OF_STOCK);
    }

    @Test
    void staysQuietForASiteAboveItsOwnReorderPointEvenWhenTheGroupIsLow() {
        Product product = Product.builder().id(12L).quantity(5).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        siteHolds(product, warehouse, 4, 2);

        service(notifierA).evaluate(product, warehouse);

        verify(notifierA, never()).send(any());
    }

    @Test
    void measuresTheProductTotalWhenTheSiteNamesNoReorderPoint() {
        Product product = Product.builder().id(13L).sku("SKU-13").name("Widget")
                .quantity(5).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        siteHolds(product, warehouse, 4, null);

        service(notifierA).evaluate(product, warehouse);

        StockEventNotification sent = captureFrom(notifierA);
        assertThat(sent.quantity()).isEqualTo(5);
        assertThat(sent.reorderThreshold()).isEqualTo(10);
        assertThat(sent.warehouseCode()).isNull();
    }

    @Test
    void measuresTheProductTotalWhenTheSiteHoldsNoLevelAtAll() {
        Product product = Product.builder().id(14L).sku("SKU-14").name("Widget")
                .quantity(5).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        lenient().when(stockLevelRepository.findByProductIdAndWarehouseId(14L, 7L)).thenReturn(Optional.empty());

        service(notifierA).evaluate(product, warehouse);

        assertThat(captureFrom(notifierA).warehouseCode()).isNull();
    }

    @Test
    void relocationAlertsTheSiteItEmptied() {
        Product product = Product.builder().id(15L).sku("SKU-15").name("Widget")
                .quantity(40).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        siteHolds(product, warehouse, 1, 5);

        service(notifierA).evaluateRelocation(product, warehouse);

        StockEventNotification sent = captureFrom(notifierA);
        assertThat(sent.eventType()).isEqualTo(StockEventType.LOW_STOCK);
        assertThat(sent.warehouseCode()).isEqualTo("WH-NORTH");
        assertThat(sent.quantity()).isEqualTo(1);
    }

    @Test
    void relocationNeverFallsBackToTheProductTotal() {
        Product product = Product.builder().id(16L).quantity(5).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        siteHolds(product, warehouse, 4, null);

        service(notifierA).evaluateRelocation(product, warehouse);

        verify(notifierA, never()).send(any());
    }

}
