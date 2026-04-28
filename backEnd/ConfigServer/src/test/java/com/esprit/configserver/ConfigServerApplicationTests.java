package com.esprit.configserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ConfigServerApplicationTests {

    @Test
    void mainRunsWithoutThrowing() {
        assertDoesNotThrow(() -> ConfigServerApplication.main(new String[]{
            "--spring.main.web-application-type=none",
            "--spring.cloud.config.server.bootstrap=false"
        }));
    }
}
