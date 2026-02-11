package com.esprit.keycloak.service;

import com.esprit.keycloak.client.UserServiceClient;
import com.esprit.keycloak.config.KeycloakProperties;
import com.esprit.keycloak.dto.RegisterRequest;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Uses Keycloak Admin API to create users and assign realm roles.
 * Realm roles should match the User service: CLIENT, FREELANCER, ADMIN.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final KeycloakProperties keycloakProperties;
    private final UserServiceClient userServiceClient;

    /**
     * Create a Keycloak admin client for the master realm (to obtain token and call Admin API).
     */
    public Keycloak createAdminKeycloak() {
        KeycloakProperties.Admin admin = keycloakProperties.getAdmin();
        KeycloakBuilder builder = KeycloakBuilder.builder()
            .serverUrl(keycloakProperties.getAuthServerUrl())
            .realm(admin.getRealm())
            .username(admin.getUsername())
            .password(admin.getPassword())
            .clientId(admin.getClientId());
        if (admin.getClientSecret() != null && !admin.getClientSecret().isBlank()) {
            builder.clientSecret(admin.getClientSecret());
        }
        return builder.build();
    }

    /**
     * Create a user in Keycloak and assign the given realm role.
     * Role must exist in the realm (CLIENT, FREELANCER, ADMIN).
     *
     * @return Keycloak user ID (UUID) if created
     * @throws IllegalArgumentException if role is invalid or user already exists
     */
    public String registerUser(RegisterRequest request) {
        String realm = keycloakProperties.getRealm();
        String roleName = request.getRole().toUpperCase();
        validateRole(roleName);

        try (Keycloak keycloak = createAdminKeycloak()) {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            if (usersResource.search(request.getEmail(), true).stream().anyMatch(u -> request.getEmail().equalsIgnoreCase(u.getEmail()))) {
                throw new IllegalArgumentException("User with email already exists: " + request.getEmail());
            }

            UserRepresentation user = new UserRepresentation();
            user.setUsername(request.getEmail());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEnabled(true);
            user.setEmailVerified(true);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getPassword());
            credential.setTemporary(false);
            user.setCredentials(Collections.singletonList(credential));

            try (Response response = usersResource.create(user)) {
                if (response.getStatus() != 201) {
                    String error = response.readEntity(String.class);
                    log.warn("Keycloak create user failed: {} - {}", response.getStatus(), error);
                    throw new IllegalStateException("Failed to create user in Keycloak: " + error);
                }
                String path = response.getLocation().getPath();
                String userId = path.substring(path.lastIndexOf('/') + 1);
                assignRealmRole(keycloak, realm, userId, roleName);
                log.info("Created Keycloak user {} with role {}", request.getEmail(), roleName);

                try {
                    userServiceClient.createUser(request);
                } catch (Exception e) {
                    try {
                        realmResource.users().get(userId).remove();
                        log.warn("Rolled back Keycloak user {} after User service failure", request.getEmail());
                    } catch (Exception rollbackEx) {
                        log.error("Failed to rollback Keycloak user {}: {}", request.getEmail(), rollbackEx.getMessage());
                    }
                    throw e;
                }
                return userId;
            }
        }
    }

    private void validateRole(String roleName) {
        List<String> allowed = List.of("CLIENT", "FREELANCER", "ADMIN");
        if (!allowed.contains(roleName)) {
            throw new IllegalArgumentException("Invalid role. Allowed: " + allowed);
        }
    }

    private void assignRealmRole(Keycloak keycloak, String realm, String userId, String roleName) {
        RealmResource realmResource = keycloak.realm(realm);
        RoleRepresentation roleRep = realmResource.roles().get(roleName).toRepresentation();
        UserResource userResource = realmResource.users().get(userId);
        userResource.roles().realmLevel().add(Collections.singletonList(roleRep));
    }
}
