package com.esprit.keycloak.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String bearerJwt = "bearer-jwt";
        return new OpenAPI()
            .servers(List.of(
                new Server().url("http://localhost:8081").description("Keycloak Auth service")))
            .addSecurityItem(new SecurityRequirement().addList(bearerJwt))
            .components(new Components()
                .addSecuritySchemes(bearerJwt,
                    new SecurityScheme()
                        .name(bearerJwt)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Keycloak JWT access token")));
    }
}
