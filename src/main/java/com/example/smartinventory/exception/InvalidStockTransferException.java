package com.example.smartinventory.exception;

/** Thrown when a stock transfer names locations that cannot take part in one. */
public class InvalidStockTransferException extends RuntimeException {

    public InvalidStockTransferException(String message) {
        super(message);
    }

}
