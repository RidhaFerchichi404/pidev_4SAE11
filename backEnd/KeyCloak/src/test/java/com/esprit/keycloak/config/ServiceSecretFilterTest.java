package com.esprit.keycloak.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceSecretFilterTest {

    @Test
    void deniesWhenSecretMissing() throws ServletException, IOException {
        KeycloakProperties properties = new KeycloakProperties();
        properties.setServiceSecret("");
        ServiceSecretFilter filter = new ServiceSecretFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/auth/admin/users/by-email/a@b.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(403, response.getStatus());
    }

    @Test
    void allowsWithCorrectHeader() throws ServletException, IOException {
        KeycloakProperties properties = new KeycloakProperties();
        properties.setServiceSecret("s3cret");
        ServiceSecretFilter filter = new ServiceSecretFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/auth/admin/users/by-email/a@b.com");
        request.addHeader("X-Service-Secret", "s3cret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(200, response.getStatus());
    }
}
