package com.esprit.keycloak.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Security configuration: OAuth2 Resource Server with Keycloak JWT.
 * Public: /api/auth/register, /api/auth/token, /api/auth/admin/users/by-email/**, actuator, error.
 * Protected (JWT): /api/auth/userinfo, /api/auth/validate, POST /api/auth/admin/users (ADMIN), and any other /api/**.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/api/auth/register", "/api/auth/token", "/actuator/**", "/error").permitAll()
                .requestMatchers("/api/auth/admin/users/by-email/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/admin/users").hasRole("ADMIN")
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    /**
     * Map Keycloak realm roles and resource roles to Spring Security authorities.
     * Reads realm_access.roles and roles from every client in resource_access.
     */
    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            List<String> realmRoles = realmAccess != null && realmAccess.get("roles") instanceof List
                ? ((List<?>) realmAccess.get("roles")).stream().map(Object::toString).toList()
                : List.of();

            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            List<String> resourceRoles = List.of();
            if (resourceAccess != null) {
                resourceRoles = resourceAccess.values().stream()
                    .filter(v -> v instanceof Map)
                    .flatMap(client -> {
                        Object roles = ((Map<?, ?>) client).get("roles");
                        if (roles instanceof List) {
                            return ((List<?>) roles).stream().map(Object::toString);
                        }
                        return Stream.empty();
                    })
                    .distinct()
                    .toList();
            }

            return Stream.concat(realmRoles.stream(), resourceRoles.stream())
                .distinct()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
        };
    }
}
