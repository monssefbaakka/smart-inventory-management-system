package com.example.smartinventory.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.smartinventory.dto.StockAvailabilityResponse;
import com.example.smartinventory.dto.StockReservationRequest;
import com.example.smartinventory.exception.InsufficientStockException;
import com.example.smartinventory.exception.InvalidReservationStateException;
import com.example.smartinventory.exception.ResourceNotFoundException;
import com.example.smartinventory.model.MovementType;
import com.example.smartinventory.model.Product;
import com.example.smartinventory.model.ReservationStatus;
import com.example.smartinventory.model.StockReservation;
import com.example.smartinventory.model.Warehouse;
import com.example.smartinventory.repository.StockReservationRepository;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    private static final Product PRODUCT = Product.builder().id(1L).sku("SKU-1").name("Widget").quantity(40).build();
    private static final Warehouse WAREHOUSE = Warehouse.builder().id(7L).code("WH-1").build();

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private StockLevelService stockLevelService;

    @Mock
    private StockMovementService stockMovementService;

    @InjectMocks
    private StockReservationService stockReservationService;

    @Test
    void reserveHoldsStockAgainstTheProductTotal() {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(stockReservationRepository.sumHeldForProduct(eq(1L), any(Instant.class))).thenReturn(0L);
        when(stockReservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        StockReservation reservation = stockReservationService.reserve(1L,
                new StockReservationRequest("SO-1042", 12, null, null));

        assertThat(reservation.getProduct()).isSameAs(PRODUCT);
        assertThat(reservation.getWarehouse()).isNull();
        assertThat(reservation.getReference()).isEqualTo("SO-1042");
        assertThat(reservation.getQuantity()).isEqualTo(12);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(reservation.getExpiresAt()).isNull();
    }

    @Test
    void reserveKeepsTheExpiryTheCallerAskedFor() {
        Instant tomorrow = Instant.now().plusSeconds(86400);
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(stockReservationRepository.sumHeldForProduct(eq(1L), any(Instant.class))).thenReturn(0L);
        when(stockReservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        StockReservation reservation = stockReservationService.reserve(1L,
                new StockReservationRequest("SO-1042", 12, null, tomorrow));

        assertThat(reservation.getExpiresAt()).isEqualTo(tomorrow);
    }

    @Test
    void reserveCountsWhatIsAlreadyHeldAsUnavailable() {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(stockReservationRepository.sumHeldForProduct(eq(1L), any(Instant.class))).thenReturn(35L);

        assertThatThrownBy(() -> stockReservationService.reserve(1L,
                new StockReservationRequest("SO-1042", 12, null, null)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("only 5 available");

        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void reserveInAWarehouseIsCheckedAgainstThatLocationRatherThanTheProductTotal() {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(warehouseService.findById(7L)).thenReturn(WAREHOUSE);
        when(stockLevelService.quantityOnHand(1L, 7L)).thenReturn(5);
        when(stockReservationRepository.sumHeldForProductInWarehouse(eq(1L), eq(7L), any(Instant.class)))
                .thenReturn(0L);

        assertThatThrownBy(() -> stockReservationService.reserve(1L,
                new StockReservationRequest("SO-1042", 12, 7L, null)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("WH-1")
                .hasMessageContaining("only 5 available");

        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void reserveInAWarehouseHoldsThatLocationsStock() {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(warehouseService.findById(7L)).thenReturn(WAREHOUSE);
        when(stockLevelService.quantityOnHand(1L, 7L)).thenReturn(20);
        when(stockReservationRepository.sumHeldForProductInWarehouse(eq(1L), eq(7L), any(Instant.class)))
                .thenReturn(4L);
        when(stockReservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        StockReservation reservation = stockReservationService.reserve(1L,
                new StockReservationRequest("SO-1042", 12, 7L, null));

        assertThat(reservation.getWarehouse()).isSameAs(WAREHOUSE);
    }

    @Test
    void availabilityBreaksTheProductTotalDown() {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(stockReservationRepository.sumHeldForProduct(eq(1L), any(Instant.class))).thenReturn(12L);

        StockAvailabilityResponse availability = stockReservationService.availability(1L, null);

        assertThat(availability.productId()).isEqualTo(1L);
        assertThat(availability.warehouseId()).isNull();
        assertThat(availability.onHand()).isEqualTo(40);
        assertThat(availability.reserved()).isEqualTo(12);
        assertThat(availability.available()).isEqualTo(28);
    }

    @Test
    void availabilityScopedToAWarehouseReportsThatLocationsLevel() {
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(warehouseService.findById(7L)).thenReturn(WAREHOUSE);
        when(stockLevelService.quantityOnHand(1L, 7L)).thenReturn(20);
        when(stockReservationRepository.sumHeldForProductInWarehouse(eq(1L), eq(7L), any(Instant.class)))
                .thenReturn(5L);

        StockAvailabilityResponse availability = stockReservationService.availability(1L, 7L);

        assertThat(availability.warehouseId()).isEqualTo(7L);
        assertThat(availability.onHand()).isEqualTo(20);
        assertThat(availability.reserved()).isEqualTo(5);
        assertThat(availability.available()).isEqualTo(15);
    }

    @Test
    void availabilityNeverGoesBelowZeroWhenReservedStockHasBeenShippedOut() {
        when(productService.findById(1L)).thenReturn(Product.builder().id(1L).quantity(4).build());
        when(stockReservationRepository.sumHeldForProduct(eq(1L), any(Instant.class))).thenReturn(10L);

        StockAvailabilityResponse availability = stockReservationService.availability(1L, null);

        assertThat(availability.available()).isZero();
        assertThat(availability.reserved()).isEqualTo(10);
    }

    @Test
    void findByProductReturnsEveryReservationWhenNoStatusIsAskedFor() {
        StockReservation reservation = held(3L, null);
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(stockReservationRepository.findByProduct(1L)).thenReturn(List.of(reservation));

        assertThat(stockReservationService.findByProduct(1L, null)).containsExactly(reservation);
    }

    @Test
    void findByProductNarrowsToOneStatus() {
        StockReservation reservation = held(3L, null);
        when(productService.findById(1L)).thenReturn(PRODUCT);
        when(stockReservationRepository.findByProductAndStatus(1L, ReservationStatus.HELD))
                .thenReturn(List.of(reservation));

        assertThat(stockReservationService.findByProduct(1L, ReservationStatus.HELD)).containsExactly(reservation);
    }

    @Test
    void findByIdRejectsAnUnknownReservation() {
        when(stockReservationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockReservationService.findById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9");
    }

    @Test
    void releaseGivesTheHeldStockBackWithoutMovingAnything() {
        StockReservation reservation = held(3L, null);
        when(stockReservationRepository.findById(3L)).thenReturn(Optional.of(reservation));
        when(stockReservationRepository.save(reservation)).thenReturn(reservation);

        StockReservation released = stockReservationService.release(3L);

        assertThat(released.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        verifyNoInteractions(stockMovementService);
    }

    @Test
    void releaseSettlesAHoldThatHasAlreadyLapsed() {
        StockReservation reservation = held(3L, Instant.now().minusSeconds(60));
        when(stockReservationRepository.findById(3L)).thenReturn(Optional.of(reservation));
        when(stockReservationRepository.save(reservation)).thenReturn(reservation);

        assertThat(stockReservationService.release(3L).getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void releaseRejectsAReservationThatWasAlreadyFulfilled() {
        StockReservation reservation = held(3L, null);
        reservation.setStatus(ReservationStatus.FULFILLED);
        when(stockReservationRepository.findById(3L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> stockReservationService.release(3L))
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessageContaining("FULFILLED");

        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void fulfilRecordsTheOutwardMovementAndClosesTheReservation() {
        StockReservation reservation = held(3L, null);
        reservation.setWarehouse(WAREHOUSE);
        when(stockReservationRepository.findById(3L)).thenReturn(Optional.of(reservation));
        when(stockReservationRepository.save(reservation)).thenReturn(reservation);

        StockReservation fulfilled = stockReservationService.fulfil(3L);

        assertThat(fulfilled.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
        verify(stockMovementService).record(eq(1L), eq(7L), isNull(), eq(MovementType.OUT), eq(12),
                eq("Fulfilled reservation SO-1042"));
    }

    @Test
    void fulfilOfAHoldTakenWithoutALocationMovesTheProductTotal() {
        StockReservation reservation = held(3L, null);
        when(stockReservationRepository.findById(3L)).thenReturn(Optional.of(reservation));
        when(stockReservationRepository.save(reservation)).thenReturn(reservation);

        stockReservationService.fulfil(3L);

        verify(stockMovementService).record(eq(1L), isNull(), isNull(), eq(MovementType.OUT), eq(12),
                eq("Fulfilled reservation SO-1042"));
    }

    @Test
    void fulfilRejectsAHoldThatHasLapsed() {
        StockReservation reservation = held(3L, Instant.now().minusSeconds(60));
        when(stockReservationRepository.findById(3L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> stockReservationService.fulfil(3L))
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessageContaining("lapsed");

        verifyNoInteractions(stockMovementService);
        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void fulfilRejectsAReservationThatWasAlreadyReleased() {
        StockReservation reservation = held(3L, null);
        reservation.setStatus(ReservationStatus.RELEASED);
        when(stockReservationRepository.findById(3L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> stockReservationService.fulfil(3L))
                .isInstanceOf(InvalidReservationStateException.class)
                .hasMessageContaining("RELEASED");

        verifyNoInteractions(stockMovementService);
    }

    @Test
    void fulfilLeavesTheReservationHeldWhenTheStockIsNoLongerThere() {
        StockReservation reservation = held(3L, null);
        when(stockReservationRepository.findById(3L)).thenReturn(Optional.of(reservation));
        when(stockMovementService.record(eq(1L), isNull(), isNull(), eq(MovementType.OUT), eq(12),
                any(String.class))).thenThrow(new InsufficientStockException("only 4 in stock"));

        assertThatThrownBy(() -> stockReservationService.fulfil(3L))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HELD);
        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    private static StockReservation held(Long id, Instant expiresAt) {
        return StockReservation.builder()
                .id(id)
                .product(PRODUCT)
                .reference("SO-1042")
                .quantity(12)
                .status(ReservationStatus.HELD)
                .expiresAt(expiresAt)
                .build();
    }

}
