package com.rameshkumar.placementsystem.controller;

import com.rameshkumar.placementsystem.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated endpoints used to wake the service on Render's free tier,
 * as the Render health check, and by the frontend's keep-alive ping.
 */
@Tag(name = "Health APIs", description = "Liveness and readiness checks")
@RestController
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Operation(summary = "Service information", description = "Landing response for the backend root URL.")
    @GetMapping("/")
    public ApiResponse<Map<String, Object>> root() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "Placement Management System API");
        info.put("status", "UP");
        info.put("health", "/health");
        info.put("readiness", "/health/ready");
        info.put("docs", "/swagger-ui/index.html");
        return new ApiResponse<>(true, "Placement Management System API is running", info);
    }

    @Operation(summary = "Liveness check", description = "Cheap check that does not touch the database. Used by Render and the frontend wake-up ping.")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return new ApiResponse<>(true, "Service is up", baseStatus("UP"));
    }

    @Operation(summary = "Readiness check", description = "Verifies database connectivity. Returns 503 when the database is unreachable.")
    @GetMapping("/health/ready")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ready() {
        Map<String, Object> status = baseStatus("UP");
        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            status.put("database", "UP");
            return ResponseEntity.ok(new ApiResponse<>(true, "Service and database are ready", status));
        } catch (RuntimeException ex) {
            logger.warn("Readiness check failed: {}", ex.getMessage());
            status.put("status", "DEGRADED");
            status.put("database", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResponse<>(false, "Database is not reachable", status));
        }
    }

    private static Map<String, Object> baseStatus(String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status);
        data.put("timestamp", Instant.now().toString());
        data.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        return data;
    }
}
