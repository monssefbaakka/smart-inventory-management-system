package com.example.smartinventory.exception;

/** Thrown when a movement names a batch that cannot take part in it. */
public class InvalidBatchException extends RuntimeException {

    public InvalidBatchException(String message) {
        super(message);
    }

}
