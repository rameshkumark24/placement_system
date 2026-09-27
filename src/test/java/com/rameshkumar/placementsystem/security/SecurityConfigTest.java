package com.rameshkumar.placementsystem.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    @Test
    void normalizesConfiguredCorsOrigins() {
        List<String> origins = SecurityConfig.normalizeOrigins(List.of(
                " https://placement.vercel.app/ ",
                "https://placement.vercel.app",
                "",
                "https://*.vercel.app//",
                "http://localhost:5173"));

        assertEquals(List.of(
                "https://placement.vercel.app",
                "https://*.vercel.app",
                "http://localhost:5173"), origins);
    }
}
