package com.example.smartinventory.exception;

/** Thrown when an operation targets a tenant that has been deactivated. */
public class InactiveTenantException extends RuntimeException {

    public InactiveTenantException(String message) {
        super(message);
    }

}
