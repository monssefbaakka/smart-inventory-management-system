package com.example.smartinventory.exception;

/** Thrown when a request carries a query parameter the endpoint cannot honour. */
public class InvalidQueryParameterException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message description of what the caller asked for and why it cannot be honoured
     */
    public InvalidQueryParameterException(String message) {
        super(message);
    }

}
