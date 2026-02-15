package com.esprit.user.client;

import com.esprit.user.config.KeycloakAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Calls keycloak-auth service to sync delete/update with Keycloak (so userdb and Keycloak stay in sync).
 * If keycloak.auth.service-url or service-secret is not set, operations are no-ops.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthClient {

    private final KeycloakAuthProperties keycloakAuthProperties;
    private final RestTemplate restTemplate;

    /**
     * Delete user from Keycloak by email. Best-effort: logs and continues if keycloak-auth is unreachable.
     * No-op if not configured or email blank.
     */
    public void deleteUserByEmail(String email) {
        if (email == null || email.isBlank()) return;
        if (!keycloakAuthProperties.isConfigured()) return;
        String base = keycloakAuthProperties.getServiceUrl().replaceAll("/$", "");
        String url = base + "/api/auth/admin/users/by-email/" + encodePathSegment(email);
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers()), Void.class);
            log.info("Synced delete to Keycloak for email: {}", email);
        } catch (Exception e) {
            log.warn("Keycloak delete failed for {}: {}", email, e.getMessage());
        }
    }

    public void updateUserByEmail(String currentEmail, String firstName, String lastName, String newEmail, String role) {
        if (!keycloakAuthProperties.isConfigured() || currentEmail == null || currentEmail.isBlank()) return;
        String base = keycloakAuthProperties.getServiceUrl().replaceAll("/$", "");
        String url = base + "/api/auth/admin/users/by-email/" + encodePathSegment(currentEmail);
        try {
            Map<String, String> body = new HashMap<>();
            body.put("firstName", firstName != null ? firstName : "");
            body.put("lastName", lastName != null ? lastName : "");
            body.put("email", newEmail != null ? newEmail : "");
            body.put("role", role != null ? role : "");
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers()), Void.class);
            log.info("Synced update to Keycloak for email: {}", currentEmail);
        } catch (Exception e) {
            log.warn("Failed to sync update to Keycloak for {}: {}", currentEmail, e.getMessage());
        }
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Service-Secret", keycloakAuthProperties.getServiceSecret());
        h.set("Content-Type", "application/json");
        return h;
    }

    private static String encodePathSegment(String s) {
        return UriUtils.encodePathSegment(s, StandardCharsets.UTF_8);
    }
}
