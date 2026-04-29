package com.example.review.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOnlyFilterTest {

    private final GatewayOnlyFilter filter = new GatewayOnlyFilter();

    @Test
    void allowsWhitelistedEndpointsAndOptions() throws ServletException, IOException {
        MockHttpServletRequest health = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse healthResp = new MockHttpServletResponse();
        filter.doFilter(health, healthResp, new MockFilterChain());
        assertThat(healthResp.getStatus()).isEqualTo(200);

        MockHttpServletRequest options = new MockHttpServletRequest("OPTIONS", "/api/reviews");
        MockHttpServletResponse optionsResp = new MockHttpServletResponse();
        filter.doFilter(options, optionsResp, new MockFilterChain());
        assertThat(optionsResp.getStatus()).isEqualTo(200);
    }

    @Test
    void blocksDirectCallsWithoutGatewayHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reviews");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).contains("Direct access");
    }

    @Test
    void allowsCallsWithGatewayHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reviews");
        request.addHeader("X-Internal-Gateway", "true");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
