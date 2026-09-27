package com.rameshkumar.placementsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail(){ return email; }

    // Trimmed before validation: mobile keyboards often append a space after autocompleting an email.
    public void setEmail(String email){ this.email = email == null ? null : email.trim(); }

    public String getPassword(){ return password; }

    public void setPassword(String password){ this.password = password; }
}
