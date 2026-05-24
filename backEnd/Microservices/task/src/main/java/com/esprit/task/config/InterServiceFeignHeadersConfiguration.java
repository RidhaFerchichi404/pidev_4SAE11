package com.esprit.task.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Downstream microservices enforce {@link GatewayOnlyFilter} (header {@code X-Internal-Gateway: true}).
 * Browser traffic gets that header from the API gateway; outbound Feign calls from Task must add it too.
 * Resource servers (Project, Planning, Contract, etc.) also expect the end-user JWT when applicable.
 */
@Configuration
public class InterServiceFeignHeadersConfiguration {

    @Bean
    public RequestInterceptor interServiceGatewayAndAuthHeadersInterceptor() {
        return template -> {
            template.header("X-Internal-Gateway", "true");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth && jwtAuth.getToken() != null) {
                template.header("Authorization", "Bearer " + jwtAuth.getToken().getTokenValue());
            }
        };
    }
}
