package com.esprit.keycloak.service;

import com.esprit.keycloak.client.UserServiceClient;
import com.esprit.keycloak.config.KeycloakProperties;
import com.esprit.keycloak.dto.RegisterRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeycloakAdminServiceTest {

    @Test
    void registerUser_rejectsInvalidRole() {
        KeycloakAdminService service = new KeycloakAdminService(keycloakProps(), mock(UserServiceClient.class));
        RegisterRequest req = validRequest();
        req.setRole("UNKNOWN");

        assertThatThrownBy(() -> service.registerUser(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid role");
    }

    @Test
    void registerUser_throwsWhenUserAlreadyExists() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);

        UserRepresentation existing = new UserRepresentation();
        existing.setEmail("user@mail.com");

        when(keycloak.realm("smart-freelance")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.search("user@mail.com", true)).thenReturn(List.of(existing));

        KeycloakAdminService service = spy(new KeycloakAdminService(keycloakProps(), mock(UserServiceClient.class)));
        doReturn(keycloak).when(service).createAdminKeycloak();
        RegisterRequest request = validRequest();

        assertThatThrownBy(() -> service.registerUser(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void registerUser_rollsBackKeycloakUserWhenUserServiceFails() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource createdUser = mock(UserResource.class);
        RolesResource roles = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        var roleScope = mock(org.keycloak.admin.client.resource.RoleScopeResource.class);
        Response response = mock(Response.class);

        when(keycloak.realm("smart-freelance")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(realm.roles()).thenReturn(roles);
        when(users.search("user@mail.com", true)).thenReturn(List.of());
        when(users.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("http://kc/users/new-id"));
        RoleMappingResource roleMapping = mock(RoleMappingResource.class);
        when(users.get("new-id")).thenReturn(createdUser);
        when(createdUser.roles()).thenReturn(roleMapping);
        when(roleMapping.realmLevel()).thenReturn(roleScope);
        when(roles.get("CLIENT")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation("CLIENT", null, false));

        UserServiceClient userServiceClient = mock(UserServiceClient.class);
        doThrow(new RuntimeException("user service down")).when(userServiceClient).createUser(any(RegisterRequest.class));

        KeycloakAdminService service = spy(new KeycloakAdminService(keycloakProps(), userServiceClient));
        doReturn(keycloak).when(service).createAdminKeycloak();
        RegisterRequest request = validRequest();

        assertThatThrownBy(() -> service.registerUser(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("user service down");

        verify(createdUser).remove();
    }

    @Test
    void sendForgotPasswordEmail_throwsWhenUserNotFound() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);

        when(keycloak.realm("smart-freelance")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.search("missing@mail.com", true)).thenReturn(List.of());
        when(users.search("missing@mail.com", false)).thenReturn(List.of());

        KeycloakAdminService service = spy(new KeycloakAdminService(keycloakProps(), mock(UserServiceClient.class)));
        doReturn(keycloak).when(service).createAdminKeycloak();

        assertThatThrownBy(() -> service.sendForgotPasswordEmail("missing@mail.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No user found");
    }

    @Test
    void sendForgotPasswordEmail_executesUpdatePasswordAction() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);

        UserRepresentation user = new UserRepresentation();
        user.setId("u-1");
        user.setEmail("user@mail.com");

        when(keycloak.realm("smart-freelance")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.search("user@mail.com", true)).thenReturn(List.of(user));
        when(users.get("u-1")).thenReturn(userResource);

        KeycloakAdminService service = spy(new KeycloakAdminService(keycloakProps(), mock(UserServiceClient.class)));
        doReturn(keycloak).when(service).createAdminKeycloak();

        service.sendForgotPasswordEmail("user@mail.com");

        verify(userResource).executeActionsEmail(List.of("UPDATE_PASSWORD"));
    }

    @Test
    void sendForgotPasswordEmail_requiresEmail() {
        KeycloakAdminService service = new KeycloakAdminService(keycloakProps(), mock(UserServiceClient.class));

        assertThatThrownBy(() -> service.sendForgotPasswordEmail(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email is required");
    }

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@mail.com");
        request.setPassword("strongPass123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setRole("CLIENT");
        return request;
    }

    private KeycloakProperties keycloakProps() {
        KeycloakProperties properties = new KeycloakProperties();
        properties.setAuthServerUrl("http://localhost:8180");
        properties.setRealm("smart-freelance");
        properties.setResource("smart-freelance-backend");

        KeycloakProperties.Admin admin = new KeycloakProperties.Admin();
        admin.setRealm("master");
        admin.setClientId("admin-cli");
        admin.setUsername("admin");
        admin.setPassword("admin");
        properties.setAdmin(admin);
        return properties;
    }
}
