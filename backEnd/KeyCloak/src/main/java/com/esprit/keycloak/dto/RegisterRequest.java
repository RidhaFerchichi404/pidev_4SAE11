package com.esprit.keycloak.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for user registration in Keycloak.
 * Aligns with User service: email, password, firstName, lastName, role (CLIENT, FREELANCER, ADMIN).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    /**
     * Role in the platform: CLIENT, FREELANCER, or ADMIN.
     */
    @NotBlank
    private String role;
}
