package com.esprit.keycloak;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyCloakApplicationTests {

    @Test
    void hasSpringBootApplicationAnnotation() {
        assertTrue(KeyCloakApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }

}
