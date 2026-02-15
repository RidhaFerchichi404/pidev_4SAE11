package com.esprit.keycloak.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * For /api/auth/admin/** requests, requires X-Service-Secret header to match
 * keycloak.service-secret (used by User service for sync delete/update).
 */
@Component
@Order(-100)
@RequiredArgsConstructor
public class ServiceSecretFilter extends OncePerRequestFilter {

    private final KeycloakProperties keycloakProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(request.getContextPath() + "/api/auth/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String secret = keycloakProperties.getServiceSecret();
        if (secret == null || secret.isBlank()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Service secret not configured");
            return;
        }
        String header = request.getHeader("X-Service-Secret");
        if (!secret.equals(header)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing X-Service-Secret");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
