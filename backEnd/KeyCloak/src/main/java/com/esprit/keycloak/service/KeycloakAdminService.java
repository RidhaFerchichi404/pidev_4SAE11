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
        String roleName = request.getRole() != null ? request.getRole().toUpperCase() : "";
        validateRole(roleName);

        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }

        try (Keycloak keycloak = createAdminKeycloak()) {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            boolean alreadyExists = usersResource.search(email, true).stream()
                .anyMatch(u -> u.getEmail() != null && email.equalsIgnoreCase(u.getEmail().trim()));
            if (alreadyExists) {
                throw new IllegalArgumentException("User with email already exists: " + email);
            }

            UserRepresentation user = new UserRepresentation();
            user.setUsername(email);
            user.setEmail(email);
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
                log.info("Created Keycloak user {} with role {}", email, roleName);

                try {
                    userServiceClient.createUser(request);
                } catch (Exception e) {
                    try {
                        realmResource.users().get(userId).remove();
                        log.warn("Rolled back Keycloak user {} after User service failure", email);
                    } catch (Exception rollbackEx) {
                        log.error("Failed to rollback Keycloak user {}: {}", email, rollbackEx.getMessage());
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

    /** Remove our app roles from user and assign the single new role (CLIENT, FREELANCER, ADMIN). */
    private void setRealmRole(Keycloak keycloak, String realm, String userId, String newRoleName) {
        RealmResource realmResource = keycloak.realm(realm);
        var roleMapping = realmResource.users().get(userId).roles().realmLevel();
        List<String> appRoles = List.of("CLIENT", "FREELANCER", "ADMIN");
        for (String r : appRoles) {
            try {
                RoleRepresentation roleRep = realmResource.roles().get(r).toRepresentation();
                roleMapping.remove(Collections.singletonList(roleRep));
            } catch (Exception ignored) { /* role not assigned */ }
        }
        RoleRepresentation newRole = realmResource.roles().get(newRoleName).toRepresentation();
        roleMapping.add(Collections.singletonList(newRole));
    }

    /**
     * Delete a user from Keycloak by email (username).
     * No-op if user does not exist.
     */
    public void deleteUserByEmail(String email) {
        if (email == null || email.isBlank()) return;
        String realm = keycloakProperties.getRealm();
        String search = email.trim();
        try (Keycloak keycloak = createAdminKeycloak()) {
            List<UserRepresentation> found = keycloak.realm(realm).users().search(search, true);
            if (found.isEmpty()) {
                found = keycloak.realm(realm).users().search(search, false);
            }
            for (UserRepresentation u : found) {
                if (search.equalsIgnoreCase(u.getEmail()) || search.equalsIgnoreCase(u.getUsername())) {
                    keycloak.realm(realm).users().get(u.getId()).remove();
                    log.info("Deleted Keycloak user by email: {}", email);
                    return;
                }
            }
            log.debug("No Keycloak user found for email: {} (realm={})", email, realm);
        }
    }

    /**
     * Update a user in Keycloak by current email (username).
     * Updates firstName, lastName, email (username), and optionally realm role.
     */
    public void updateUserByEmail(String currentEmail, String firstName, String lastName, String newEmail, String roleName) {
        if (currentEmail == null || currentEmail.isBlank()) return;
        String realm = keycloakProperties.getRealm();
        try (Keycloak keycloak = createAdminKeycloak()) {
            List<UserRepresentation> found = keycloak.realm(realm).users().search(currentEmail.trim(), true);
            for (UserRepresentation u : found) {
                if (currentEmail.equalsIgnoreCase(u.getEmail()) || currentEmail.equalsIgnoreCase(u.getUsername())) {
                    if (firstName != null) u.setFirstName(firstName);
                    if (lastName != null) u.setLastName(lastName);
                    if (newEmail != null && !newEmail.isBlank()) {
                        u.setEmail(newEmail);
                        u.setUsername(newEmail);
                    }
                    keycloak.realm(realm).users().get(u.getId()).update(u);
                    if (roleName != null && !roleName.isBlank()) {
                        validateRole(roleName.toUpperCase());
                        setRealmRole(keycloak, realm, u.getId(), roleName.toUpperCase());
                    }
                    log.info("Updated Keycloak user: {} -> firstName={}, lastName={}, email={}, role={}", currentEmail, firstName, lastName, newEmail, roleName);
                    return;
                }
            }
            log.warn("No Keycloak user found for update by email: {}", currentEmail);
        }
    }
}
