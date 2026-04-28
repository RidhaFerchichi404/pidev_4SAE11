package com.esprit.notification.service;

import com.esprit.notification.dto.NotificationRequest;
import com.esprit.notification.dto.NotificationResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryNotificationStoreTest {

    @Test
    void createAndFindByUserId_returnsNewestFirst() throws InterruptedException {
        InMemoryNotificationStore store = new InMemoryNotificationStore();

        NotificationResponse first = store.create(NotificationRequest.builder()
                .userId("u-1")
                .title("first")
                .data(Map.of("a", "1"))
                .build());
        Thread.sleep(5);
        NotificationResponse second = store.create(NotificationRequest.builder()
                .userId("u-1")
                .title("second")
                .build());

        List<NotificationResponse> result = store.findByUserId("u-1");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(second.getId());
        assertThat(result.get(1).getId()).isEqualTo(first.getId());
    }

    @Test
    void markReadAndDelete_behaveAsExpected() {
        InMemoryNotificationStore store = new InMemoryNotificationStore();
        NotificationResponse created = store.create(NotificationRequest.builder()
                .userId("u-2")
                .title("title")
                .build());

        NotificationResponse marked = store.markRead(created.getId());
        store.delete(created.getId());
        NotificationResponse missing = store.markRead(created.getId());

        assertThat(marked.isRead()).isTrue();
        assertThat(missing).isNull();
    }
}
