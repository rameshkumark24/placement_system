package com.rameshkumar.placementsystem.controller;

import com.rameshkumar.placementsystem.dto.ApiResponse;
import com.rameshkumar.placementsystem.dto.AuthResponse;
import com.rameshkumar.placementsystem.dto.LoginRequest;
import com.rameshkumar.placementsystem.dto.RegisterRequest;
import com.rameshkumar.placementsystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication APIs", description = "Endpoints for user registration and login")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account with the default STUDENT role.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or registration request is invalid")
    })
    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request){
        String result = authService.register(request);
        return new ApiResponse<>(true, "User registered successfully", result);
    }

    @Operation(summary = "Authenticate user and return JWT token", description = "Validates user credentials and returns a JWT token for authenticated requests.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid email or password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        AuthResponse authResponse = authService.login(request);
        return new ApiResponse<>(true, "Login successful", authResponse);
    }
}
