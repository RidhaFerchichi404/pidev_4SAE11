package com.esprit.planning.service;

import com.esprit.planning.client.NotificationClient;
import com.esprit.planning.dto.NotificationRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sends notifications to the Notification microservice.
 * Failures are logged but do not affect the calling flow (fire-and-forget).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanningNotificationService {

    private final NotificationClient notificationClient;

    public static final String TYPE_PROGRESS_UPDATE = "PROGRESS_UPDATE";
    public static final String TYPE_PROGRESS_COMMENT = "PROGRESS_COMMENT";
    /** Calendar: project deadline added to calendar */
    public static final String TYPE_CALENDAR_DEADLINE = "CALENDAR_DEADLINE";
    /** Calendar: next progress update due reminder */
    public static final String TYPE_CALENDAR_REMINDER = "CALENDAR_REMINDER";
    /** Calendar: milestone (e.g. 100%) reached */
    public static final String TYPE_CALENDAR_MILESTONE = "CALENDAR_MILESTONE";

    /**
     * Notify a user. Catches and logs any exception so Planning is not affected if Notification is down.
     */
    public void notifyUser(String userId, String title, String body, String type, Map<String, String> data) {
        if (userId == null || title == null) {
            return;
        }
        try {
            NotificationRequestDto request = NotificationRequestDto.builder()
                .userId(userId)
                .title(title)
                .body(body != null ? body : "")
                .type(type != null ? type : "GENERAL")
                .data(data)
                .build();
            notificationClient.create(request);
        } catch (Exception e) {
            log.warn("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }
}
