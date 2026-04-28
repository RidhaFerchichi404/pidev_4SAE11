package com.esprit.keycloak.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigBeansTest {

    @Test
    void restTemplateBeanIsCreated() {
        RestTemplateConfig config = new RestTemplateConfig();
        RestTemplate template = config.restTemplate();
        assertNotNull(template);
    }

    @Test
    void openApiBeanContainsBearerScheme() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.openAPI();
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get("bearer-jwt"));
    }

    @Test
    void keycloakPropertiesDefaultsAreSet() {
        KeycloakProperties props = new KeycloakProperties();
        assertEquals("smart-freelance", props.getRealm());
        assertEquals("", props.getServiceSecret());
    }
}
