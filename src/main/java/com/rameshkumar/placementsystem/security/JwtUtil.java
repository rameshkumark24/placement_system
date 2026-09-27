package com.rameshkumar.placementsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    public static final String DEV_SECRET = "placementSystemJwtSecretKey2026SecureKey123!";
    private static final int MIN_SECRET_BYTES = 32;

    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final Key signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration-ms:1800000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        // HS256 needs at least 256 bits. Failing here gives a clear startup error instead of
        // a WeakKeyException on the first login attempt.
        if (secretKey == null || secretKey.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long for HS256 signing");
        }
        if (DEV_SECRET.equals(secretKey)) {
            logger.warn("Using the built-in development JWT secret. Set JWT_SECRET to a private value in production.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(String username, String role) {
        return generateToken(username, role, ACCESS_TOKEN_TYPE, accessTokenExpirationMs);
    }

    public String generateRefreshToken(String username, String role) {
        return generateToken(username, role, REFRESH_TOKEN_TYPE, refreshTokenExpirationMs);
    }

    private String generateToken(String username, String role, String tokenType, long expirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(ROLE_CLAIM, role);
        claims.put(TOKEN_TYPE_CLAIM, tokenType);

        Date issuedAt = new Date();
        Date expiryDate = new Date(issuedAt.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(issuedAt)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // EXTRACT USERNAME
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // EXTRACT ROLE
    public String extractRole(String token) {
        return parseClaims(token).get(ROLE_CLAIM, String.class);
    }

    public String extractTokenType(String token) {
        return parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims,T> claimsResolver) {
        Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Verifies the signature and expiry and returns the claims.
     * Throws {@link io.jsonwebtoken.ExpiredJwtException} or another {@link io.jsonwebtoken.JwtException}
     * when the token cannot be trusted.
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isAccessToken(Claims claims) {
        return ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public String getRole(Claims claims) {
        return claims.get(ROLE_CLAIM, String.class);
    }

    public Boolean validateToken(String token, String username) {
        Claims claims = parseClaims(token);
        return claims.getSubject().equals(username)
                && claims.getExpiration().after(new Date())
                && isAccessToken(claims);
    }

    public Boolean validateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().after(new Date())
                && REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
