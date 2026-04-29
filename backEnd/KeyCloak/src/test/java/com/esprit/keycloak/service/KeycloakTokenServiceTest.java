package com.esprit.keycloak.service;

import com.esprit.keycloak.config.KeycloakProperties;
import com.esprit.keycloak.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakTokenServiceTest {

    @Test
    void getToken_returnsParsedTokens() {
        KeycloakTokenService service = new KeycloakTokenService(keycloakProps());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8080/realms/smart/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"acc\",\"refresh_token\":\"ref\",\"token_type\":\"Bearer\",\"expires_in\":300}",
                        MediaType.APPLICATION_JSON
                ));

        TokenResponse response = service.getToken("user", "pwd");

        assertThat(response.getAccessToken()).isEqualTo("acc");
        assertThat(response.getRefreshToken()).isEqualTo("ref");
        server.verify();
    }

    @Test
    void getToken_mapsUnauthorizedFromKeycloak() {
        KeycloakTokenService service = new KeycloakTokenService(keycloakProps());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8080/realms/smart/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error_description\":\"Bad credentials\"}"));

        assertThatThrownBy(() -> service.getToken("user", "wrong"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(UNAUTHORIZED);
    }

    private KeycloakProperties keycloakProps() {
        KeycloakProperties properties = new KeycloakProperties();
        properties.setAuthServerUrl("http://localhost:8080");
        properties.setRealm("smart");
        properties.setResource("frontend-app");
        KeycloakProperties.Credentials credentials = new KeycloakProperties.Credentials();
        credentials.setSecret("secret");
        properties.setCredentials(credentials);
        return properties;
    }
}
