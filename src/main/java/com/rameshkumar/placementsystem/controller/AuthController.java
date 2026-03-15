package com.rameshkumar.placementsystem.controller;

import com.rameshkumar.placementsystem.dto.ApiResponse;
import com.rameshkumar.placementsystem.dto.AuthResponse;
import com.rameshkumar.placementsystem.dto.LoginRequest;
import com.rameshkumar.placementsystem.dto.RegisterRequest;
import com.rameshkumar.placementsystem.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request){
        String result = authService.register(request);
        return new ApiResponse<>(true, "User registered successfully", result);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        AuthResponse authResponse = authService.login(request);
        return new ApiResponse<>(true, "Login successful", authResponse);
    }
}
