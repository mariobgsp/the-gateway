package com.example.gatewayservice.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtRequestFilterTest {

    private static class TestableFilter extends JwtRequestFilter {
        public boolean shouldSkip(HttpServletRequest request) {
            return shouldNotFilter(request);
        }
    }

    @Test
    void shouldNotFilterLoginEndpoint() {
        TestableFilter filter = new TestableFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/gateway/user/login");
        assertTrue(filter.shouldSkip(request));
    }

    @Test
    void shouldFilterProtectedEndpoints() {
        TestableFilter filter = new TestableFilter();
        assertFalse(filter.shouldSkip(new MockHttpServletRequest("GET", "/api/gateway/getApiList")));
        assertFalse(filter.shouldSkip(new MockHttpServletRequest("GET", "/api/gateway/gateway-catapi")));
        assertFalse(filter.shouldSkip(new MockHttpServletRequest("POST", "/api/store/getList")));
    }
}
