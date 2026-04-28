package com.esprit.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayErrorWebExceptionHandlerTest {

    private final GatewayErrorWebExceptionHandler handler = new GatewayErrorWebExceptionHandler();

    @Test
    void returnsUnauthorizedWhenJwtErrorDetected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/secure").build());

        handler.handle(exchange, new RuntimeException("jwt expired")).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());
    }

    @Test
    void returnsBadGatewayWhenConnectionRefused() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/planning").build());

        handler.handle(exchange, new RuntimeException("Connection refused")).block();

        assertEquals(502, exchange.getResponse().getStatusCode().value());
        String contentType = exchange.getResponse().getHeaders().getFirst("Content-Type");
        assertTrue(contentType != null && contentType.contains("application/json"));
    }

    @Test
    void returnsBadGatewayWhenTimeoutDetected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/planning").build());
        handler.handle(exchange, new RuntimeException("connection timed out")).block();
        assertEquals(502, exchange.getResponse().getStatusCode().value());
    }
}
