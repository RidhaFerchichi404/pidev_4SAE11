package com.example.review.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reviewServiceAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8085");
        localServer.setDescription("Server URL in Local environment");

        Contact contact = new Contact();
        contact.setName("Review Microservice Team");
        contact.setEmail("review@example.com");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Review Microservice API")
                .version("1.0")
                .contact(contact)
                .description("This API exposes endpoints to manage reviews and review responses.")
                .termsOfService("https://example.com/terms")
                .license(mitLicense);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
