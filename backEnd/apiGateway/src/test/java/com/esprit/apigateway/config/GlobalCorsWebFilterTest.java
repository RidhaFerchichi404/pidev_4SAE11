package com.esprit.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalCorsWebFilterTest {

    private final GlobalCorsWebFilter filter = new GlobalCorsWebFilter();

    @Test
    void preflightRequestReturnsNoContentWithCorsHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.OPTIONS, "/anything")
            .header(HttpHeaders.ORIGIN, "http://localhost:4200")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, e -> Mono.empty()).block();

        assertEquals(204, exchange.getResponse().getStatusCode().value());
        assertEquals("http://localhost:4200",
            exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void allowedOriginDecoratesResponseHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/anything")
            .header(HttpHeaders.ORIGIN, "http://localhost:4200")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        WebFilterChain chain = serverWebExchange -> {
            chainCalled.set(true);
            serverWebExchange.getResponse().setComplete();
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertTrue(chainCalled.get());
    }

    @Test
    void disallowedOriginPassesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
            .method(HttpMethod.GET, "/anything")
            .header(HttpHeaders.ORIGIN, "http://evil.com")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, e -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chainCalled.get());
    }
}
