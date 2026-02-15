package com.esprit.apigateway.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Adds CORS headers to every response (including errors like 500) so the browser
 * does not block the response and the frontend can see the real status/body.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalCorsWebFilter implements WebFilter {

    private static final String ALLOWED_ORIGIN = "http://localhost:4200";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();

        String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
        if (ALLOWED_ORIGIN.equals(origin)) {
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN);
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type, Accept, Origin, X-Requested-With");
        }

        return chain.filter(exchange);
    }
}
