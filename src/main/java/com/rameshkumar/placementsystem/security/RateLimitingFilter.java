package com.rameshkumar.placementsystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rameshkumar.placementsystem.dto.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final long capacity;
    private final long refillTokens;
    private final long refillDurationMinutes;

    public RateLimitingFilter(
            ObjectMapper objectMapper,
            @Value("${rate-limit.capacity:20}") long capacity,
            @Value("${rate-limit.refill-tokens:20}") long refillTokens,
            @Value("${rate-limit.refill-duration-minutes:1}") long refillDurationMinutes) {
        this.objectMapper = objectMapper;
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDurationMinutes = refillDurationMinutes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = resolveClientKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        logger.warn("Rate limit exceeded for client {} on {}", key, request.getRequestURI());
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(new ApiResponse<>(false, "Too many requests. Please try again later.", null))
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html");
    }

    private Bucket newBucket() {
        Refill refill = Refill.greedy(refillTokens, Duration.ofMinutes(refillDurationMinutes));
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket.builder()
                .addLimit(limit)
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
