package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Basic user information fetched from the User microservice")
public class UserDto {

    @Schema(description = "User ID", example = "5")
    private Long id;

    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Schema(description = "User last name", example = "Doe")
    private String lastName;

    @Schema(description = "User email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "User role or type", example = "FREELANCER")
    private String role;
}

