package com.example.smartinventory.exception;

/** Thrown when a stock-out movement would reduce a product's quantity below zero. */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }

}
