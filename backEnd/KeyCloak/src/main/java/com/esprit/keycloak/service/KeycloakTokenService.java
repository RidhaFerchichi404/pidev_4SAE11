package com.esprit.keycloak.service;

import com.esprit.keycloak.config.KeycloakProperties;
import com.esprit.keycloak.dto.TokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Proxies token requests to Keycloak (resource owner password grant).
 * Clients can POST /api/auth/token with username/password to get JWT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakTokenService {

    private final KeycloakProperties keycloakProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TokenResponse getToken(String username, String password) {
        String url = keycloakProperties.getAuthServerUrl() + "/realms/" + keycloakProperties.getRealm()
            + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", keycloakProperties.getResource());
        body.add("username", username);
        body.add("password", password);
        String secret = keycloakProperties.getCredentials().getSecret();
        if (secret != null && !secret.isBlank()) {
            body.add("client_secret", secret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalArgumentException("Token request failed: " + response.getStatusCode());
        }

        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            return TokenResponse.builder()
                .accessToken(node.path("access_token").asText(null))
                .refreshToken(node.has("refresh_token") ? node.path("refresh_token").asText() : null)
                .tokenType(node.path("token_type").asText("Bearer"))
                .expiresIn(node.has("expires_in") ? node.path("expires_in").asInt() : null)
                .refreshExpiresIn(node.has("refresh_expires_in") ? node.path("refresh_expires_in").asInt() : null)
                .scope(node.has("scope") ? node.path("scope").asText() : null)
                .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse token response", e);
        }
    }
}
