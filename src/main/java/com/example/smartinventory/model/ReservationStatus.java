package com.example.smartinventory.model;

/** Lifecycle status of a {@link StockReservation}. */
public enum ReservationStatus {

    /** Holding stock against a commitment; the units are on hand but no longer available. */
    HELD,

    /** Given back before it was shipped; the units are available again and nothing moved. */
    RELEASED,

    /** Shipped; the units left through an {@code OUT} movement recorded for the reservation. */
    FULFILLED
}
