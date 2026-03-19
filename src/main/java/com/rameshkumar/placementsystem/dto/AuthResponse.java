package com.rameshkumar.placementsystem.dto;

public class AuthResponse {

    private String token;
    private String refreshToken;
    private long expiresInMs;

    public AuthResponse(String token, String refreshToken, long expiresInMs){
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresInMs = expiresInMs;
    }

    public String getToken(){
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }
}
