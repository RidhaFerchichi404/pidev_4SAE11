package com.esprit.user.controller;

import com.esprit.user.dto.UserRequest;
import com.esprit.user.dto.UserResponse;
import com.esprit.user.dto.UserUpdateRequest;
import com.esprit.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for User management in the User microservice.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for managing users (CRUD) in the Smart Freelance and Project Matching platform")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get all users", description = "Returns a list of all registered users.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserResponse> getAllUsers() {
        try {
            return userService.findAll().stream()
                    .map(UserResponse::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getAllUsers failed - check database and logs", e);
            return List.of();
        }
    }

    @Operation(summary = "Get user by ID", description = "Returns a single user by their unique identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        return UserResponse.fromEntity(userService.findById(id));
    }

    @Operation(summary = "Get user by email", description = "Returns a single user by their email address.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping(value = "/email/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse getUserByEmail(
            @Parameter(description = "User email") @PathVariable String email) {
        return UserResponse.fromEntity(userService.findByEmail(email));
    }

    @Operation(summary = "Create user", description = "Creates a new user. Password is hashed before storage.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate email")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return UserResponse.fromEntity(userService.create(request));
    }

    @Operation(summary = "Update user", description = "Updates an existing user by ID. Only provided fields are updated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate email")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return UserResponse.fromEntity(userService.update(id, request));
    }

    @Operation(summary = "Delete user", description = "Deletes a user by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        userService.deleteById(id);
    }
}
