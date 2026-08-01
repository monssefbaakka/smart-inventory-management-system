package com.example.smartinventory.exception;

/** Thrown when an operation conflicts with the state a batch is already in. */
public class InvalidBatchStateException extends RuntimeException {

    public InvalidBatchStateException(String message) {
        super(message);
    }

}
