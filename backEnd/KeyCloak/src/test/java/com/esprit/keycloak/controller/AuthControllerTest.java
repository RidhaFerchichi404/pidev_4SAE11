package com.esprit.keycloak.controller;

import com.esprit.keycloak.dto.RegisterRequest;
import com.esprit.keycloak.dto.TokenRequest;
import com.esprit.keycloak.dto.TokenResponse;
import com.esprit.keycloak.dto.KeycloakUserUpdateRequest;
import com.esprit.keycloak.service.KeycloakAdminService;
import com.esprit.keycloak.service.KeycloakTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void registerReturnsCreatedPayload() {
        KeycloakAdminService adminService = mock(KeycloakAdminService.class);
        KeycloakTokenService tokenService = mock(KeycloakTokenService.class);
        when(adminService.registerUser(any(RegisterRequest.class))).thenReturn("kc-id");
        AuthController controller = new AuthController(adminService, tokenService);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@mail.com");
        req.setPassword("secret");
        req.setRole("CLIENT");

        Map<String, String> result = controller.register(req);
        assertEquals("kc-id", result.get("keycloakUserId"));
    }

    @Test
    void tokenDelegatesToService() {
        KeycloakAdminService adminService = mock(KeycloakAdminService.class);
        KeycloakTokenService tokenService = mock(KeycloakTokenService.class);
        TokenResponse expected = new TokenResponse("a", "r", "Bearer", 60, 300, "openid");
        when(tokenService.getToken("user", "pass")).thenReturn(expected);
        AuthController controller = new AuthController(adminService, tokenService);

        TokenRequest request = new TokenRequest();
        request.setUsername("user");
        request.setPassword("pass");

        TokenResponse result = controller.token(request);
        assertEquals("a", result.getAccessToken());
    }

    @Test
    void userinfoBuildsRolesFromRealmAndResource() {
        AuthController controller = new AuthController(mock(KeycloakAdminService.class), mock(KeycloakTokenService.class));
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "sub-1")
            .claim("email", "user@mail.com")
            .claim("given_name", "First")
            .claim("family_name", "Last")
            .claim("preferred_username", "preferred")
            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
            .claim("resource_access", Map.of("smart-freelance-backend", Map.of("roles", List.of("CLIENT"))))
            .expiresAt(Instant.now().plusSeconds(60))
            .build();

        var result = controller.userinfo(jwt);
        assertTrue(result.getRoles().contains("ADMIN"));
        assertTrue(result.getRoles().contains("CLIENT"));
    }

    @Test
    void validateReturnsExpectedMap() {
        AuthController controller = new AuthController(mock(KeycloakAdminService.class), mock(KeycloakTokenService.class));
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "sub-1")
            .expiresAt(Instant.now().plusSeconds(60))
            .build();

        Map<String, Object> result = controller.validate(jwt);
        assertEquals(true, result.get("valid"));
        assertEquals("sub-1", result.get("sub"));
    }

    @Test
    void deleteAndUpdateDelegateToAdminService() {
        KeycloakAdminService adminService = mock(KeycloakAdminService.class);
        AuthController controller = new AuthController(adminService, mock(KeycloakTokenService.class));

        controller.deleteUserByEmail("a@b.com");
        controller.updateUserByEmail("a@b.com", new KeycloakUserUpdateRequest());
        verify(adminService).deleteUserByEmail("a@b.com");
        verify(adminService).updateUserByEmail("a@b.com", null, null, null, null);
    }

    @Test
    void refreshRequiresToken() {
        AuthController controller = new AuthController(mock(KeycloakAdminService.class), mock(KeycloakTokenService.class));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> controller.refresh(Map.of()));
    }

    @Test
    void adminCreateDelegatesToAdminService() {
        KeycloakAdminService adminService = mock(KeycloakAdminService.class);
        when(adminService.registerUser(any(RegisterRequest.class))).thenReturn("id-2");
        AuthController controller = new AuthController(adminService, mock(KeycloakTokenService.class));
        RegisterRequest req = new RegisterRequest();
        req.setEmail("admin@x.com");
        req.setPassword("secret");
        req.setRole("ADMIN");

        Map<String, String> result = controller.adminCreateUser(req);
        assertEquals("id-2", result.get("keycloakUserId"));
    }
}
