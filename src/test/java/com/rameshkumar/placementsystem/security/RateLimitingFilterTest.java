package com.rameshkumar.placementsystem.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        // 3 API requests and 2 auth requests per minute per client.
        filter = new RateLimitingFilter(new ObjectMapper(), 3, 3, 1, 2, 1);
    }

    @Test
    void blocksApiRequestsOverCapacityWithRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(200, send("GET", "/companies", "10.0.0.1").getStatus());
        }

        MockHttpServletResponse blocked = send("GET", "/companies", "10.0.0.1");
        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));
        assertTrue(blocked.getContentAsString().contains("Too many requests"));

        // Other clients have their own bucket.
        assertEquals(200, send("GET", "/companies", "10.0.0.2").getStatus());
    }

    @Test
    void authEndpointsUseASeparateStricterBucket() throws Exception {
        assertEquals(200, send("POST", "/auth/login", "10.0.0.3").getStatus());
        assertEquals(200, send("POST", "/auth/login", "10.0.0.3").getStatus());
        assertEquals(429, send("POST", "/auth/login", "10.0.0.3").getStatus());

        // Exhausting the login bucket does not block normal API usage.
        assertEquals(200, send("GET", "/companies", "10.0.0.3").getStatus());
    }

    @Test
    void healthChecksAndPreflightsAreNeverLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, send("GET", "/health", "10.0.0.4").getStatus());
            assertEquals(200, send("OPTIONS", "/companies", "10.0.0.4").getStatus());
        }
        assertEquals(200, send("GET", "/", "10.0.0.4").getStatus());
    }

    @Test
    void usesFirstForwardedForAddressAsClientKey() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = request("GET", "/companies", "10.9.9.9");
            request.addHeader("X-Forwarded-For", "203.0.113.7, 10.9.9.9");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }
        MockHttpServletRequest request = request("GET", "/companies", "10.1.1.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(429, response.getStatus());
    }

    private MockHttpServletResponse send(String method, String path, String remoteAddr) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(method, path, remoteAddr), response, new MockFilterChain());
        return response;
    }

    private static MockHttpServletRequest request(String method, String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
