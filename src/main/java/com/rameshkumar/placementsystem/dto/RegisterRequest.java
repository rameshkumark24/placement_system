package com.rameshkumar.placementsystem.dto;

import jakarta.validation.constraints.*;

public class RegisterRequest {

    @NotBlank(message = "Name cannot be empty")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    // BCrypt only uses the first 72 bytes of a password.
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    private String password;

    public RegisterRequest() {
    }

    public RegisterRequest(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public String getName(){ return name; }

    public void setName(String name){ this.name = name; }

    public String getEmail(){ return email; }

    // Trimmed before validation: mobile keyboards often append a space after autocompleting an email.
    public void setEmail(String email){ this.email = email == null ? null : email.trim(); }

    public String getPassword(){ return password; }

    public void setPassword(String password){ this.password = password; }
}
