package com.rameshkumar.placementsystem.exception;

/**
 * Signals invalid credentials or an unusable token (HTTP 401).
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
