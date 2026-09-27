package com.rameshkumar.placementsystem.security;

/**
 * Paths that are reachable without a JWT. Shared by the security chain and the custom filters
 * so the lists cannot drift apart.
 */
public final class PublicEndpoints {

    /** Liveness/wake-up endpoints: always public, never rate limited, no JWT processing. */
    public static final String[] HEALTH = {
            "/",
            "/health",
            "/health/**",
            "/error"
    };

    /** API documentation. */
    public static final String[] DOCS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    public static final String AUTH = "/auth/**";

    private PublicEndpoints() {
    }

    public static boolean isHealthPath(String path) {
        return path.equals("/")
                || path.equals("/health")
                || path.startsWith("/health/")
                || path.equals("/error");
    }

    public static boolean isDocsPath(String path) {
        return path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    public static boolean isAuthPath(String path) {
        return path.startsWith("/auth/");
    }
}
