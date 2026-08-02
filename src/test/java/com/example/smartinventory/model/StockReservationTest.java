package com.example.smartinventory.model;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class StockReservationTest {

    private static final Instant NOON = Instant.parse("2026-08-02T12:00:00Z");

    @Test
    void onCreateStampsTimesAndDefaultsToHeld() {
        StockReservation reservation = new StockReservation();
        reservation.setStatus(null);

        reservation.onCreate();

        assertThat(reservation.getCreatedAt()).isNotNull();
        assertThat(reservation.getUpdatedAt()).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HELD);
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        StockReservation reservation = new StockReservation();

        reservation.onUpdate();

        assertThat(reservation.getUpdatedAt()).isNotNull();
    }

    @Test
    void aHoldWithNoExpiryNeverLapses() {
        StockReservation reservation = held(null);

        assertThat(reservation.isExpiredAt(NOON)).isFalse();
        assertThat(reservation.holdsStockAt(NOON)).isTrue();
    }

    @Test
    void aHoldLapsesOnceItsExpiryHasBeenReached() {
        assertThat(held(NOON.plusSeconds(60)).isExpiredAt(NOON)).isFalse();
        assertThat(held(NOON).isExpiredAt(NOON)).isTrue();
        assertThat(held(NOON.minusSeconds(60)).isExpiredAt(NOON)).isTrue();
    }

    @Test
    void aLapsedHoldStopsHoldingItsStock() {
        assertThat(held(NOON.minusSeconds(60)).holdsStockAt(NOON)).isFalse();
    }

    @Test
    void aSettledReservationHasAnOutcomeRatherThanAnExpiry() {
        StockReservation released = held(NOON.minusSeconds(60));
        released.setStatus(ReservationStatus.RELEASED);
        StockReservation fulfilled = held(NOON.minusSeconds(60));
        fulfilled.setStatus(ReservationStatus.FULFILLED);

        assertThat(released.isExpiredAt(NOON)).isFalse();
        assertThat(fulfilled.isExpiredAt(NOON)).isFalse();
        assertThat(released.holdsStockAt(NOON)).isFalse();
        assertThat(fulfilled.holdsStockAt(NOON)).isFalse();
    }

    private static StockReservation held(Instant expiresAt) {
        return StockReservation.builder()
                .id(3L)
                .product(Product.builder().id(1L).build())
                .reference("SO-1042")
                .quantity(12)
                .status(ReservationStatus.HELD)
                .expiresAt(expiresAt)
                .build();
    }

}
