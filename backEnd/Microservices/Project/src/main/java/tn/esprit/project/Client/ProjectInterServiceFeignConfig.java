package tn.esprit.project.Client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * For server-to-server Feign calls to task/review: same contract as browser traffic via gateway
 * ({@code X-Internal-Gateway}) plus forwarded JWT for OAuth2 resource servers.
 */
public class ProjectInterServiceFeignConfig {

    @Bean
    public RequestInterceptor projectInterServiceHeadersInterceptor() {
        return template -> {
            template.header("X-Internal-Gateway", "true");
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                String token = jwtAuth.getToken().getTokenValue();
                template.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
        };
    }
}
