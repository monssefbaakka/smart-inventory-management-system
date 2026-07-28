package com.example.smartinventory.exception;

/** Thrown when a stock-count operation is not allowed from the count's current status. */
public class InvalidStockCountStateException extends RuntimeException {

    public InvalidStockCountStateException(String message) {
        super(message);
    }

}
