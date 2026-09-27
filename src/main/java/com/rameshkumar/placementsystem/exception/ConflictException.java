package com.rameshkumar.placementsystem.exception;

/**
 * Signals a request that conflicts with existing data, such as a duplicate email (HTTP 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
