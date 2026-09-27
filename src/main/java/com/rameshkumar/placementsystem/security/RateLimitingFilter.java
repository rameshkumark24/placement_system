package com.rameshkumar.placementsystem.security;

import com.rameshkumar.placementsystem.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client token-bucket rate limiting. Authentication endpoints get their own, stricter bucket
 * to slow down password guessing without throttling normal dashboard usage.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Upper bound on tracked clients so the maps cannot grow without limit.
    static final int MAX_TRACKED_CLIENTS = 10_000;

    private final ConcurrentMap<String, Bucket> apiBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final long capacity;
    private final long refillTokens;
    private final Duration refillPeriod;
    private final long authCapacity;
    private final Duration authRefillPeriod;

    public RateLimitingFilter(
            ObjectMapper objectMapper,
            @Value("${rate-limit.capacity:100}") long capacity,
            @Value("${rate-limit.refill-tokens:100}") long refillTokens,
            @Value("${rate-limit.refill-duration-minutes:1}") long refillDurationMinutes,
            @Value("${rate-limit.auth-capacity:20}") long authCapacity,
            @Value("${rate-limit.auth-refill-duration-minutes:1}") long authRefillDurationMinutes) {
        this.objectMapper = objectMapper;
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriod = Duration.ofMinutes(refillDurationMinutes);
        this.authCapacity = authCapacity;
        this.authRefillPeriod = Duration.ofMinutes(authRefillDurationMinutes);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean authRequest = PublicEndpoints.isAuthPath(request.getRequestURI());
        ConcurrentMap<String, Bucket> buckets = authRequest ? authBuckets : apiBuckets;
        if (buckets.size() >= MAX_TRACKED_CLIENTS) {
            buckets.clear();
        }

        String key = resolveClientKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> authRequest ? newAuthBucket() : newApiBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
        logger.warn("Rate limit exceeded for client {} on {}", key, request.getRequestURI());
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(new ApiResponse<>(false,
                        "Too many requests. Please try again in " + retryAfterSeconds + " seconds.", null))
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || PublicEndpoints.isHealthPath(path)
                || PublicEndpoints.isDocsPath(path);
    }

    private Bucket newApiBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(refillTokens, refillPeriod)
                        .build())
                .build();
    }

    private Bucket newAuthBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(authCapacity)
                        .refillGreedy(authCapacity, authRefillPeriod)
                        .build())
                .build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
