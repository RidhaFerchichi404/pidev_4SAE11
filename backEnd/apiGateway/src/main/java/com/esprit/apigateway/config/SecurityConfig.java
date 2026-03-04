package com.esprit.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /** Public routes: no JWT so browser never gets 401 that can appear as status 0. */
    @Bean
    @Order(1)
    public SecurityWebFilterChain publicChain(ServerHttpSecurity http) {
        return http
                .securityMatcher(new ServerWebExchangeMatcher() {
                    @Override
                    public Mono<ServerWebExchangeMatcher.MatchResult> matches(ServerWebExchange exchange) {
                        String path = exchange.getRequest().getPath().value();
                        boolean match = exchange.getRequest().getMethod() == HttpMethod.OPTIONS
                                || path.startsWith("/planning/")
                                || path.startsWith("/project/")
                                || path.startsWith("/task/")
                                || path.startsWith("/offer/")
                                || path.startsWith("/review/")
                                || path.startsWith("/notification/")
                                || path.startsWith("/portfolio/")
                                || path.startsWith("/contract/")
                                || path.startsWith("/keycloak-auth/")
                                || path.startsWith("/user/api/users/email/")
                                || path.startsWith("/user/api/users/avatars/")
                                || "/actuator/health".equals(path)
                                || "/actuator/info".equals(path);
                        return match ? ServerWebExchangeMatcher.MatchResult.match() : ServerWebExchangeMatcher.MatchResult.notMatch();
                    }
                })
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }

    /** Protected routes: require JWT. */
    @Bean
    @Order(2)
    public SecurityWebFilterChain protectedChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/**").authenticated()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }
}
