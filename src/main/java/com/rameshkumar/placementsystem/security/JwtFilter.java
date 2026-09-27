package com.rameshkumar.placementsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;
    private final JsonSecurityErrorHandler securityErrorHandler;

    public JwtFilter(JwtUtil jwtUtil, JsonSecurityErrorHandler securityErrorHandler) {
        this.jwtUtil = jwtUtil;
        this.securityErrorHandler = securityErrorHandler;
    }

    // Public endpoints must keep working even if the browser still holds a stale token.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || PublicEndpoints.isHealthPath(path)
                || PublicEndpoints.isDocsPath(path)
                || PublicEndpoints.isAuthPath(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims;
        try {
            claims = jwtUtil.parseClaims(authHeader.substring(7));
        } catch (ExpiredJwtException ex) {
            logger.debug("Expired JWT received for request {}", request.getRequestURI());
            securityErrorHandler.write(response, HttpServletResponse.SC_UNAUTHORIZED, "JWT token has expired");
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("Invalid JWT received for request {}", request.getRequestURI());
            securityErrorHandler.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        }

        // A refresh token must never be usable as an access token.
        if (!jwtUtil.isAccessToken(claims)) {
            securityErrorHandler.write(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
            return;
        }

        String username = claims.getSubject();
        String role = jwtUtil.getRole(claims);

        if (username != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
            logger.debug("Authenticated user {} with role {} for request {}", username, role, request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
