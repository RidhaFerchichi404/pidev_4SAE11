package com.esprit.user.dto;

import com.esprit.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request body for partial user update (PUT). All fields are optional.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for partial user update; all fields optional")
public class UserUpdateRequest {

    @Email
    @Schema(description = "User email address (optional)")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "New password (optional)")
    private String password;

    @Schema(description = "First name (optional)")
    private String firstName;

    @Schema(description = "Last name (optional)")
    private String lastName;

    @Schema(description = "User role (optional)")
    private Role role;

    @Schema(description = "Phone number (optional)")
    private String phone;

    @Schema(description = "Avatar URL (optional)")
    private String avatarUrl;

    @Schema(description = "Whether the account is active (optional)")
    private Boolean isActive;
}
