package com.esprit.notification.service;

import com.esprit.notification.dto.NotificationRequest;
import com.esprit.notification.dto.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void createAndFindByUserId_usesInMemoryFallbackWhenFirestoreMissing() {
        ObjectProvider<com.google.cloud.firestore.Firestore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        NotificationService service = new NotificationService(provider);

        NotificationRequest req = NotificationRequest.builder()
                .userId("u-1")
                .title("Title")
                .body(null)
                .type(null)
                .data(Map.of("k", "v"))
                .build();

        NotificationResponse created = service.create(req);
        List<NotificationResponse> list = service.findByUserId("u-1");

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getBody()).isEqualTo("");
        assertThat(created.getType()).isEqualTo("GENERAL");
        assertThat(created.isRead()).isFalse();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getData()).containsEntry("k", "v");
    }

    @Test
    void markReadAndDelete_workInMemoryMode() {
        ObjectProvider<com.google.cloud.firestore.Firestore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        NotificationService service = new NotificationService(provider);

        NotificationResponse created = service.create(NotificationRequest.builder()
                .userId("u-2")
                .title("hello")
                .body("body")
                .type("INFO")
                .build());

        NotificationResponse marked = service.markRead(created.getId());
        service.delete(created.getId());
        List<NotificationResponse> afterDelete = service.findByUserId("u-2");

        assertThat(marked.isRead()).isTrue();
        assertThat(afterDelete).isEmpty();
    }

    @Test
    void markRead_throwsWhenNotificationNotFoundInMemoryMode() {
        ObjectProvider<com.google.cloud.firestore.Firestore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        NotificationService service = new NotificationService(provider);

        assertThatThrownBy(() -> service.markRead("missing"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }
}
