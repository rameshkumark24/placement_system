package com.rameshkumar.placementsystem.exception;

public class CompanyNotFoundException extends ResourceNotFoundException {

    public CompanyNotFoundException(String message) {
        super(message);
    }
}
