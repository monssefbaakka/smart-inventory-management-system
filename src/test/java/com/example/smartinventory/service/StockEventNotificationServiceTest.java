package com.example.smartinventory.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.example.smartinventory.repository.ProductRepository;
import com.example.smartinventory.repository.StockLevelRepository;
import com.example.smartinventory.repository.StockReservationRepository;

@ExtendWith(MockitoExtension.class)
class StockEventNotificationServiceTest {

    @Mock
    private StockEventNotifier notifierA;

    @Mock
    private StockEventNotifier notifierB;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    /**
     * Builds the service over the real measurer and a mocked reservation store, which holds nothing
     * until a test says otherwise -- so a case that is not about reservations measures the stock on
     * the shelf.
     */
    private StockEventNotificationService service(StockEventNotifier... notifiers) {
        return new StockEventNotificationService(List.of(notifiers), productRepository, stockLevelRepository,
                new AvailableStockService(stockReservationRepository));
    }

    /** Makes reservations hold {@code quantity} units of the product across every location. */
    private void heldAcrossTheGroup(Product product, int quantity) {
        lenient().when(stockReservationRepository.sumHeldForProduct(eq(product.getId()), any()))
                .thenReturn((long) quantity);
    }

    /** Makes reservations hold {@code quantity} units of the product at one site. */
    private void heldAtSite(Product product, Warehouse warehouse, int quantity) {
        lenient().when(stockReservationRepository
                        .sumHeldForProductInWarehouse(eq(product.getId()), eq(warehouse.getId()), any()))
                .thenReturn((long) quantity);
    }

    private Warehouse warehouse() {
        return Warehouse.builder().id(7L).code("WH-NORTH").build();
    }

    /**
     * Makes the given warehouse hold {@code quantity} units of the product against its own threshold.
     * The level is returned so a test can move its stock between measurements, as a second movement
     * through the same site would.
     */
    private StockLevel siteHolds(Product product, Warehouse warehouse, Integer quantity, Integer threshold) {
        StockLevel level = StockLevel.builder()
                .product(product)
                .warehouse(warehouse)
                .quantity(quantity)
                .reorderThreshold(threshold)
                .build();
        lenient().when(stockLevelRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()))
                .thenReturn(Optional.of(level));
        return level;
    }

    private List<StockEventNotification> captureAllFrom(StockEventNotifier notifier, int expected) {
        ArgumentCaptor<StockEventNotification> captor = ArgumentCaptor.forClass(StockEventNotification.class);
        verify(notifier, times(expected)).send(captor.capture());
        return captor.getAllValues();
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

    @Test
    void saysNothingASecondTimeWhileTheProductStaysLow() {
        Product product = Product.builder().id(20L).sku("SKU-20").name("Widget")
                .quantity(5).reorderThreshold(10).build();
        StockEventNotificationService service = service(notifierA);

        service.evaluate(product);
        product.setQuantity(4);
        service.evaluate(product);

        verify(notifierA, times(1)).send(any());
        assertThat(product.getAnnouncedStockEvent()).isEqualTo(StockEventType.LOW_STOCK);
    }

    @Test
    void announcesAStandingShortageAgainWhenItDeepens() {
        Product product = Product.builder().id(21L).sku("SKU-21").name("Widget")
                .quantity(5).reorderThreshold(10).build();
        StockEventNotificationService service = service(notifierA);

        service.evaluate(product);
        product.setQuantity(0);
        service.evaluate(product);

        assertThat(captureAllFrom(notifierA, 2)).extracting(StockEventNotification::eventType)
                .containsExactly(StockEventType.LOW_STOCK, StockEventType.OUT_OF_STOCK);
    }

    @Test
    void staysQuietWhileAnEmptyShelfStaysEmpty() {
        Product product = Product.builder().id(22L).sku("SKU-22").name("Widget")
                .quantity(0).reorderThreshold(10).build();
        StockEventNotificationService service = service(notifierA);

        service.evaluate(product);
        service.evaluate(product);

        verify(notifierA, times(1)).send(any());
    }

    @Test
    void lowersWhatStandsWithoutAnnouncingAPartialRestock() {
        Product product = Product.builder().id(23L).sku("SKU-23").name("Widget")
                .quantity(0).reorderThreshold(10).build();
        StockEventNotificationService service = service(notifierA);

        service.evaluate(product);
        product.setQuantity(3);
        service.evaluate(product);

        assertThat(product.getAnnouncedStockEvent()).isEqualTo(StockEventType.LOW_STOCK);
        verify(notifierA, times(1)).send(any());

        product.setQuantity(0);
        service.evaluate(product);

        assertThat(captureAllFrom(notifierA, 2)).extracting(StockEventNotification::eventType)
                .containsExactly(StockEventType.OUT_OF_STOCK, StockEventType.OUT_OF_STOCK);
    }

    @Test
    void clearsWhatStandsWhenStockRecoversAboveTheThreshold() {
        Product product = Product.builder().id(24L).sku("SKU-24").name("Widget")
                .quantity(5).reorderThreshold(10).build();
        StockEventNotificationService service = service(notifierA);

        service.evaluate(product);
        product.setQuantity(50);
        service.evaluate(product);

        assertThat(product.getAnnouncedStockEvent()).isNull();
        verify(notifierA, times(1)).send(any());

        product.setQuantity(5);
        service.evaluate(product);

        verify(notifierA, times(2)).send(any());
    }

    @Test
    void writesWhatStandsOnTheProductWhenTheTotalWasMeasured() {
        Product product = Product.builder().id(25L).sku("SKU-25").name("Widget")
                .quantity(5).reorderThreshold(10).build();

        service(notifierA).evaluate(product);

        verify(productRepository).save(product);
    }

    @Test
    void writesWhatStandsOnTheLevelOfTheSiteMeasured() {
        Product product = Product.builder().id(26L).sku("SKU-26").name("Widget")
                .quantity(40).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        StockLevel level = siteHolds(product, warehouse, 2, 5);

        service(notifierA).evaluate(product, warehouse);

        verify(stockLevelRepository).save(level);
        verify(productRepository, never()).save(any());
        assertThat(level.getAnnouncedStockEvent()).isEqualTo(StockEventType.LOW_STOCK);
        assertThat(product.getAnnouncedStockEvent()).isNull();
    }

    @Test
    void saysNothingASecondTimeWhileTheSiteStaysLow() {
        Product product = Product.builder().id(27L).sku("SKU-27").name("Widget")
                .quantity(40).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        StockLevel level = siteHolds(product, warehouse, 4, 5);
        StockEventNotificationService service = service(notifierA);

        service.evaluate(product, warehouse);
        level.setQuantity(3);
        service.evaluate(product, warehouse);

        verify(notifierA, times(1)).send(any());
    }

    @Test
    void oneSiteFallingQuietDoesNotSilenceAnother() {
        Product product = Product.builder().id(28L).sku("SKU-28").name("Widget")
                .quantity(40).reorderThreshold(10).build();
        Warehouse north = warehouse();
        Warehouse south = Warehouse.builder().id(8L).code("WH-SOUTH").build();
        siteHolds(product, north, 2, 5);
        siteHolds(product, south, 1, 5);
        StockEventNotificationService service = service(notifierA);

        service.evaluate(product, north);
        service.evaluate(product, north);
        service.evaluate(product, south);

        assertThat(captureAllFrom(notifierA, 2)).extracting(StockEventNotification::warehouseCode)
                .containsExactly("WH-NORTH", "WH-SOUTH");
    }

    @Test
    void aShortageStandingOnTheTotalDoesNotSilenceASite() {
        Product product = Product.builder().id(29L).sku("SKU-29").name("Widget")
                .quantity(5).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        siteHolds(product, warehouse, 2, 5);
        StockEventNotificationService service = service(notifierA);

        service.evaluate(product);
        service.evaluate(product, warehouse);

        assertThat(captureAllFrom(notifierA, 2)).extracting(StockEventNotification::warehouseCode)
                .containsExactly(null, "WH-NORTH");
    }

    @Test
    void remembersWhatStandsEvenWhenAChannelThrows() {
        Product product = Product.builder().id(30L).sku("SKU-30").name("Widget")
                .quantity(5).reorderThreshold(10).build();
        doThrow(new RuntimeException("boom")).when(notifierA).send(any());
        StockEventNotificationService service = service(notifierA);

        service.evaluate(product);
        service.evaluate(product);

        verify(notifierA, times(1)).send(any());
        assertThat(product.getAnnouncedStockEvent()).isEqualTo(StockEventType.LOW_STOCK);
    }

    @Test
    void relocationSaysNothingASecondTimeWhileTheSiteStaysLow() {
        Product product = Product.builder().id(31L).sku("SKU-31").name("Widget")
                .quantity(40).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        StockLevel level = siteHolds(product, warehouse, 4, 5);
        StockEventNotificationService service = service(notifierA);

        service.evaluateRelocation(product, warehouse);
        level.setQuantity(2);
        service.evaluateRelocation(product, warehouse);

        verify(notifierA, times(1)).send(any());
        assertThat(level.getAnnouncedStockEvent()).isEqualTo(StockEventType.LOW_STOCK);
    }

    @Test
    void measuresWhatIsFreeRatherThanWhatIsOnTheShelf() {
        Product product = Product.builder().id(40L).sku("SKU-40").name("Widget")
                .quantity(40).reorderThreshold(10).build();
        heldAcrossTheGroup(product, 38);

        service(notifierA).evaluate(product);

        StockEventNotification sent = captureFrom(notifierA);
        assertThat(sent.eventType()).isEqualTo(StockEventType.LOW_STOCK);
        assertThat(sent.quantity()).isEqualTo(2);
        assertThat(sent.reserved()).isEqualTo(38);
    }

    @Test
    void aShelfPromisedAwayEntirelyIsOutOfStock() {
        Product product = Product.builder().id(41L).sku("SKU-41").name("Widget")
                .quantity(5).reorderThreshold(10).build();
        heldAcrossTheGroup(product, 5);

        service(notifierA).evaluate(product);

        assertThat(captureFrom(notifierA).eventType()).isEqualTo(StockEventType.OUT_OF_STOCK);
    }

    @Test
    void anOverPromisedShelfIsOutOfStockRatherThanNegativelyStocked() {
        Product product = Product.builder().id(42L).sku("SKU-42").name("Widget")
                .quantity(2).reorderThreshold(10).build();
        heldAcrossTheGroup(product, 5);

        service(notifierA).evaluate(product);

        StockEventNotification sent = captureFrom(notifierA);
        assertThat(sent.eventType()).isEqualTo(StockEventType.OUT_OF_STOCK);
        assertThat(sent.quantity()).isZero();
    }

    @Test
    void staysQuietWhenTheHoldsLeaveEnoughFree() {
        Product product = Product.builder().id(43L).quantity(40).reorderThreshold(10).build();
        heldAcrossTheGroup(product, 5);

        service(notifierA).evaluate(product);

        verify(notifierA, never()).send(any());
    }

    @Test
    void aSiteIsMeasuredOnTheHoldsPlacedAgainstThatSite() {
        Product product = Product.builder().id(44L).sku("SKU-44").name("Widget")
                .quantity(400).reorderThreshold(10).build();
        Warehouse warehouse = warehouse();
        siteHolds(product, warehouse, 10, 5);
        heldAtSite(product, warehouse, 8);
        heldAcrossTheGroup(product, 300);

        service(notifierA).evaluate(product, warehouse);

        StockEventNotification sent = captureFrom(notifierA);
        assertThat(sent.warehouseCode()).isEqualTo("WH-NORTH");
        assertThat(sent.quantity()).isEqualTo(2);
        assertThat(sent.reserved()).isEqualTo(8);
    }

    @Test
    void freeingHeldStockClearsWhatStandsSoTheNextShortageIsAnnounced() {
        Product product = Product.builder().id(45L).sku("SKU-45").name("Widget")
                .quantity(40).reorderThreshold(10).build();
        lenient().when(stockReservationRepository.sumHeldForProduct(eq(45L), any()))
                .thenReturn(38L, 0L, 38L);
        StockEventNotificationService service = service(notifierA);

        service.evaluate(product);
        service.evaluate(product);

        assertThat(product.getAnnouncedStockEvent()).isNull();

        service.evaluate(product);

        verify(notifierA, times(2)).send(any());
    }

}
