package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.offer.client.NotificationFeignClient;
import org.example.offer.entity.NotificationType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationFeignClient notificationFeignClient;

    /**
     * Sends an in-app notification to the given user via the Notification microservice.
     *
     * @param userId     recipient user ID
     * @param type       notification type
     * @param title      notification title
     * @param body       notification body
     * @param offerId    related offer ID (optional metadata)
     * @param relatedId  related entity ID (e.g. question ID, optional metadata)
     */
    public void createNotification(Long userId, NotificationType type,
                                   String title, String body,
                                   Long offerId, Long relatedId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("userId", String.valueOf(userId));
            request.put("title", title);
            request.put("body", body);
            request.put("type", type.name());

            Map<String, String> data = new HashMap<>();
            if (offerId != null) {
                data.put("offerId", String.valueOf(offerId));
            }
            if (relatedId != null) {
                data.put("relatedId", String.valueOf(relatedId));
            }
            if (!data.isEmpty()) {
                request.put("data", data);
            }

            notificationFeignClient.create(request);
            log.info("[NOTIFICATION] Sent type={} to userId={} offerId={}", type, userId, offerId);
        } catch (Exception e) {
            log.warn("[NOTIFICATION] Failed to send notification type={} to userId={}: {}", type, userId, e.getMessage());
        }
    }
}
