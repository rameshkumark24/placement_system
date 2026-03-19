package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.*;

public interface AuthService {

    String register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

}
