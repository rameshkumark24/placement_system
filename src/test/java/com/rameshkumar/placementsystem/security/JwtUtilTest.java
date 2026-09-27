package com.rameshkumar.placementsystem.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String SECRET = "test-secret-that-is-definitely-longer-than-32-bytes";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, 120_000);

    @Test
    void rejectsSecretsTooShortForHs256AtStartup() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new JwtUtil("short-secret", 60_000, 120_000));
        assertTrue(exception.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void accessTokenCarriesSubjectRoleAndType() {
        String token = jwtUtil.generateAccessToken("admin@example.com", "ADMIN");

        Claims claims = jwtUtil.parseClaims(token);
        assertEquals("admin@example.com", claims.getSubject());
        assertEquals("ADMIN", jwtUtil.getRole(claims));
        assertTrue(jwtUtil.isAccessToken(claims));
        assertTrue(jwtUtil.validateToken(token, "admin@example.com"));
        assertFalse(jwtUtil.validateRefreshToken(token));
    }

    @Test
    void refreshTokenIsNotAnAccessToken() {
        String token = jwtUtil.generateRefreshToken("student@example.com", "STUDENT");

        assertFalse(jwtUtil.isAccessToken(jwtUtil.parseClaims(token)));
        assertTrue(jwtUtil.validateRefreshToken(token));
        assertFalse(jwtUtil.validateToken(token, "student@example.com"));
    }

    @Test
    void expiredAndForeignTokensAreRejected() {
        JwtUtil expiring = new JwtUtil(SECRET, -1_000, -1_000);
        String expired = expiring.generateAccessToken("student@example.com", "STUDENT");
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.parseClaims(expired));

        JwtUtil other = new JwtUtil("another-secret-that-is-also-longer-than-32-bytes", 60_000, 60_000);
        String foreign = other.generateAccessToken("student@example.com", "ADMIN");
        assertThrows(JwtException.class, () -> jwtUtil.parseClaims(foreign));
    }
}
