package com.esprit.user.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + (e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid"))
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccessException(DataAccessException ex) {
        log.error("Database error in User service", ex);
        String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        if (message == null) message = "Database error";
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Database unavailable. Check that MySQL is running and userdb exists. " + message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.warn("RuntimeException in User service: {}", ex.getMessage());
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (message != null) {
            if (message.contains("not found")) status = HttpStatus.NOT_FOUND;
            else if (message.contains("already exists")) status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(Map.of("error", message != null ? message : "Internal error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAnyException(Exception ex) {
        log.error("Unexpected error in User service", ex);
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error. Check User service logs for details. " + message));
    }
}
