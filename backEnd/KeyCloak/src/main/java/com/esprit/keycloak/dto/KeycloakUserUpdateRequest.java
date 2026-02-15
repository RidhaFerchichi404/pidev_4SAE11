package com.esprit.keycloak.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for updating a user in Keycloak (internal API)")
public class KeycloakUserUpdateRequest {

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "New email (username) - optional")
    private String email;

    @Schema(description = "Realm role: CLIENT, FREELANCER, or ADMIN - optional")
    private String role;
}
