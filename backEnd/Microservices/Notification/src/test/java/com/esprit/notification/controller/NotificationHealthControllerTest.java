package com.esprit.notification.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationHealthControllerTest {

    @Test
    void welcome_returnsServiceNameAndConfiguredMessage() {
        NotificationHealthController controller = new NotificationHealthController();
        ReflectionTestUtils.setField(controller, "welcomeMessage", "Hello Notification");

        ResponseEntity<Map<String, String>> response = controller.welcome();

        assertThat(response.getBody()).containsEntry("service", "notification");
        assertThat(response.getBody()).containsEntry("message", "Hello Notification");
    }
}
