package com.example.smartinventory.exception;

/** Thrown when a purchase-order lifecycle transition is not allowed from its current status. */
public class InvalidPurchaseOrderStateException extends RuntimeException {

    public InvalidPurchaseOrderStateException(String message) {
        super(message);
    }

}
