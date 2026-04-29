package com.example.review.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiAndSwaggerConfigTest {

    @Test
    void openApiConfigBuildsExpectedMetadata() {
        OpenApiConfig config = new OpenApiConfig();
        var api = config.reviewServiceAPI();

        assertThat(api.getInfo().getTitle()).isEqualTo("Review Microservice API");
        assertThat(api.getServers()).hasSize(1);
        assertThat(api.getServers().get(0).getUrl()).isEqualTo("http://localhost:8085");
    }

    @Test
    void swaggerRedirectConfigRedirectsToUi() {
        SwaggerUiRedirectConfig config = new SwaggerUiRedirectConfig();
        assertThat(config.redirectToSwaggerUi()).isEqualTo("redirect:/swagger-ui.html");
    }
}
