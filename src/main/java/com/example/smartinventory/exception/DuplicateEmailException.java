package com.example.smartinventory.exception;

/** Thrown when attempting to register a user with an email already in use. */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }

}
