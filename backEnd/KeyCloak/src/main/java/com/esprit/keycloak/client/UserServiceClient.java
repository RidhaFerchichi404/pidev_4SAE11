package com.esprit.keycloak.client;

import com.esprit.keycloak.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Calls the User microservice to create a user in userdb after Keycloak registration.
 * Uses Eureka service name "user" so the URL is http://user/api/users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private static final String USER_SERVICE_URL = "http://user/api/users";

    private final RestTemplate restTemplate;

    /**
     * Create a user in the User service (userdb). Request body matches UserRequest (email, password, firstName, lastName, role).
     */
    public void createUser(RegisterRequest request) {
        var body = new UserCreateRequest(
            request.getEmail(),
            request.getPassword(),
            request.getFirstName(),
            request.getLastName(),
            request.getRole().toUpperCase()
        );
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var entity = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(USER_SERVICE_URL, entity, Object.class);
            log.info("Created user in User service (userdb): {}", request.getEmail());
        } catch (Exception e) {
            log.error("Failed to create user in User service: {}", e.getMessage());
            throw new IllegalStateException("Failed to create user in userdb: " + e.getMessage(), e);
        }
    }

    /** DTO matching User service's UserRequest for JSON (role as string for enum). */
    public record UserCreateRequest(String email, String password, String firstName, String lastName, String role) {}
}
