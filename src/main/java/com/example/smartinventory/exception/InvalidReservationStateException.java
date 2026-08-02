package com.example.smartinventory.exception;

/** Thrown when an operation conflicts with the state a stock reservation is already in. */
public class InvalidReservationStateException extends RuntimeException {

    public InvalidReservationStateException(String message) {
        super(message);
    }

}
