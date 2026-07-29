package com.example.smartinventory.exception;

/** Thrown when attempting to create a tenant whose slug is already taken. */
public class DuplicateTenantSlugException extends RuntimeException {

    public DuplicateTenantSlugException(String message) {
        super(message);
    }

}
