package com.esprit.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiGatewayApplicationTests {

    @Test
    void hasSpringBootApplicationAnnotation() {
        assertTrue(ApiGatewayApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }

}
