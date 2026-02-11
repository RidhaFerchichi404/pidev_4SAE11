package com.esprit.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * User info returned from JWT or Keycloak userinfo endpoint.
 * Used by the User service and other microservices to identify the authenticated user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserInfoResponse {

    private String sub;           // Keycloak user ID (UUID)
    private String email;
    private String firstName;
    private String lastName;
    private String preferredUsername;
    private List<String> roles;
}
