package com.rameshkumar.placementsystem.exception;

/**
 * Signals a request that is well-formed but violates a business rule (HTTP 400).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
