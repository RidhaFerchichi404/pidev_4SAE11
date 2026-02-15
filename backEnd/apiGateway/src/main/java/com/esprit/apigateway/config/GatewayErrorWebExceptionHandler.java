package com.esprit.apigateway.config;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * When the Gateway fails (e.g. downstream unreachable), return a JSON body
 * with the error message so the frontend and logs show the real cause.
 */
@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        String path = exchange.getRequest().getPath().value();
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        HttpStatus status = isJwtAuthError(ex) ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY;

        if (status == HttpStatus.UNAUTHORIZED) {
            message = "Invalid or expired token. Please sign in again.";
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"error\":\"Gateway\",\"path\":\"" + path + "\",\"message\":\"" + escape(message) + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static boolean isJwtAuthError(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String name = t.getClass().getSimpleName();
            String msg = t.getMessage();
            if ("JwtException".equals(name) || "JwtValidationException".equals(name)
                    || (msg != null && msg.toLowerCase().contains("jwt"))) {
                return true;
            }
        }
        return false;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
