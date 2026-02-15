package com.esprit.user.dto;

import com.esprit.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Safe DTO for API responses. All fields are simple types (no LocalDateTime)
 * so Jackson serialization cannot fail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private String phone;
    private String avatarUrl;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;

    public static UserResponse fromEntity(com.esprit.user.entity.User u) {
        if (u == null) return null;
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(u.getRole())
                .phone(u.getPhone() != null ? u.getPhone() : "")
                .avatarUrl(u.getAvatarUrl() != null ? u.getAvatarUrl() : "")
                .isActive(u.getIsActive() != null ? u.getIsActive() : true)
                .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().format(ISO) : "")
                .updatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().format(ISO) : "")
                .build();
    }
}
