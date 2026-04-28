package com.esprit.keycloak.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesIllegalArgumentAsBadRequest() {
        var response = handler.handleBadRequest(new IllegalArgumentException("bad"));
        assertEquals(400, response.getStatusCode().value());
        assertEquals("bad", response.getBody().get("error"));
    }

    @Test
    void handlesResponseStatusException() {
        var response = handler.handleResponseStatus(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "nope"));
        assertEquals(401, response.getStatusCode().value());
        assertEquals("nope", response.getBody().get("error"));
    }

    @Test
    void handleAnyMapsUnauthorizedKeycloakError() {
        var response = handler.handleAny(new RuntimeException("401 Unauthorized"));
        assertEquals(502, response.getStatusCode().value());
    }

    @Test
    void handleAnyMapsGenericError() {
        var response = handler.handleAny(new RuntimeException("unexpected"));
        assertEquals(500, response.getStatusCode().value());
    }
}
