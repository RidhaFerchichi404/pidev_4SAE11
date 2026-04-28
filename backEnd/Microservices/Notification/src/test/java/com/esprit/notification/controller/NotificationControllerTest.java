package com.esprit.notification.controller;

import com.esprit.notification.dto.NotificationRequest;
import com.esprit.notification.dto.NotificationResponse;
import com.esprit.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void create_returnsCreatedResponse() {
        NotificationRequest request = NotificationRequest.builder().userId("u1").title("t").build();
        NotificationResponse response = NotificationResponse.builder()
                .id("id-1")
                .userId("u1")
                .title("t")
                .createdAt(Instant.now())
                .build();
        when(notificationService.create(request)).thenReturn(response);

        ResponseEntity<NotificationResponse> out = notificationController.create(request);

        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(out.getBody()).isEqualTo(response);
    }

    @Test
    void findMarkAndDelete_delegateToService() {
        NotificationResponse response = NotificationResponse.builder()
                .id("id-2")
                .userId("u2")
                .title("tt")
                .createdAt(Instant.now())
                .build();
        when(notificationService.findByUserId("u2")).thenReturn(List.of(response));
        when(notificationService.markRead("id-2")).thenReturn(response);

        List<NotificationResponse> found = notificationController.findByUserId("u2");
        NotificationResponse marked = notificationController.markRead("id-2");
        notificationController.delete("id-2");

        assertThat(found).hasSize(1);
        assertThat(marked.getId()).isEqualTo("id-2");
        verify(notificationService).delete("id-2");
    }
}
