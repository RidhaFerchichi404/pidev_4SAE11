package com.esprit.keycloak.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for token endpoint (password grant).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

    @NotBlank
    private String username;  // email

    @NotBlank
    private String password;

    private String grantType = "password";
}
