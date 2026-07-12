package com.example.smartinventory.exception;

/** Thrown when a requested entity cannot be found by its identifier. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}
