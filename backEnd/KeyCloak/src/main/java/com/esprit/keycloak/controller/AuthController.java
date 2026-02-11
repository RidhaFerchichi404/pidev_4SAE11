package com.esprit.keycloak.controller;

import com.esprit.keycloak.dto.RegisterRequest;
import com.esprit.keycloak.dto.TokenRequest;
import com.esprit.keycloak.dto.TokenResponse;
import com.esprit.keycloak.dto.UserInfoResponse;
import com.esprit.keycloak.service.KeycloakAdminService;
import com.esprit.keycloak.service.KeycloakTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Authentication API: register in Keycloak, obtain tokens, userinfo, validate.
 * Used by the User service and frontend for login/register and token validation.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Keycloak-based authentication for Smart Freelance platform")
public class AuthController {

    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakTokenService keycloakTokenService;

    @Operation(summary = "Register", description = "Create a new user in Keycloak with the given role (CLIENT, FREELANCER, ADMIN).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created in Keycloak"),
        @ApiResponse(responseCode = "400", description = "Invalid request or email already exists")
    })
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        String keycloakUserId = keycloakAdminService.registerUser(request);
        return Map.of(
            "message", "User registered in Keycloak. Use /api/auth/token to get tokens.",
            "keycloakUserId", keycloakUserId
        );
    }

    @Operation(summary = "Get token", description = "Obtain access and refresh tokens (password grant). Use the same email/password as registered.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens returned"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        return keycloakTokenService.getToken(request.getUsername(), request.getPassword());
    }

    @Operation(summary = "User info", description = "Return current user info from JWT. Requires Bearer token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User info"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    @GetMapping(value = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearer-jwt")
    public UserInfoResponse userinfo(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return UserInfoResponse.builder().build();
        }
        List<String> roles = Stream.concat(
            getRealmRoles(jwt).stream(),
            getResourceRoles(jwt).stream()
        ).distinct().collect(Collectors.toList());

        return UserInfoResponse.builder()
            .sub(jwt.getSubject())
            .email(jwt.getClaimAsString("email"))
            .firstName(jwt.getClaimAsString("given_name"))
            .lastName(jwt.getClaimAsString("family_name"))
            .preferredUsername(jwt.getClaimAsString("preferred_username"))
            .roles(roles)
            .build();
    }

    @Operation(summary = "Validate token", description = "Returns 200 if the Bearer token is valid. Used by other microservices to check auth.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Invalid or missing token")
    })
    @GetMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearer-jwt")
    public Map<String, Object> validate(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "valid", true,
            "sub", jwt.getSubject(),
            "exp", jwt.getExpiresAt() != null ? jwt.getExpiresAt().getEpochSecond() : 0
        );
    }

    @SuppressWarnings("unchecked")
    private List<String> getRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return Collections.emptyList();
        Object roles = realmAccess.get("roles");
        return roles instanceof List ? (List<String>) roles : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> getResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) return Collections.emptyList();
        Object client = resourceAccess.get("smart-freelance-backend");
        if (!(client instanceof Map)) return Collections.emptyList();
        Object roles = ((Map<?, ?>) client).get("roles");
        return roles instanceof List ? (List<String>) roles : Collections.emptyList();
    }
}
