package com.esprit.keycloak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class KeyCloakApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeyCloakApplication.class, args);
    }

}
