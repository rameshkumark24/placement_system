package com.rameshkumar.placementsystem.exception;

/**
 * Signals that a requested record does not exist (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
