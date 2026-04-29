package com.esprit.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOnlyFilterTest {

    private final GatewayOnlyFilter filter = new GatewayOnlyFilter();

    @Test
    void allowsWhitelistedActuatorPathWithoutGatewayHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsOptionsPreflightWithoutGatewayHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void blocksDirectApiCallsWithoutGatewayHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications/user/u-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).contains("Direct access");
    }

    @Test
    void allowsApiCallsWhenGatewayHeaderIsPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications/user/u-1");
        request.addHeader("X-Internal-Gateway", "true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
