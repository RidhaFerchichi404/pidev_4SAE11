package com.esprit.keycloak.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Pass through HTTP status set by service layer (e.g. 401, 503) — must be before the generic handler. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity
            .status(ex.getStatusCode())
            .body(Map.of("error", message != null ? message : "Request failed"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAny(Exception ex) {
        log.error("Registration failed", ex);
        String raw = ex.getMessage() != null ? ex.getMessage() : "";
        boolean keycloakUnauthorized = raw.contains("401") || raw.contains("NotAuthorized") || raw.contains("Unauthorized");
        if (keycloakUnauthorized) {
            String message = "Keycloak rejected admin login. Check keycloak.auth-server-url, keycloak.admin.username and keycloak.admin.password in application.properties (must be a valid Keycloak master-realm admin user).";
            return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", message));
        }
        String message = raw.isEmpty() ? "Registration failed. Check Keycloak is running and configured." : raw;
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", message));
    }
}
