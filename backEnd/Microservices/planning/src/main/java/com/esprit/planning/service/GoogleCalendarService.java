package com.esprit.planning.service;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Integrates with Google Calendar API: create/delete/list events.
 * No-ops when Calendar is disabled or credentials are missing (fail gracefully).
 */
@Service
@Slf4j
public class GoogleCalendarService {

    private final Calendar calendar;
    private final String defaultCalendarId;
    private final boolean enabled;

    public GoogleCalendarService(
            @Autowired(required = false) Calendar calendar,
            @Value("${google.calendar.enabled:false}") boolean enabled,
            @Value("${google.calendar.default-calendar-id:primary}") String defaultCalendarId) {
        this.calendar = calendar;
        this.enabled = enabled;
        this.defaultCalendarId = defaultCalendarId == null || defaultCalendarId.isBlank() ? "primary" : defaultCalendarId;
    }

    /** Whether Google Calendar API is available (enabled and credentials loaded). */
    public boolean isAvailable() {
        return enabled && calendar != null;
    }

    /**
     * Creates a calendar event. Runs asynchronously and logs errors without failing the caller.
     */
    @Async
    public void createEventAsync(String calendarId, String title, LocalDateTime startDateTime, LocalDateTime endDateTime, String description) {
        if (!isAvailable()) return;
        try {
            createEvent(calendarId != null && !calendarId.isBlank() ? calendarId : defaultCalendarId,
                    title, startDateTime, endDateTime, description);
        } catch (Exception e) {
            log.warn("Failed to create Google Calendar event: {}", e.getMessage());
        }
    }

    /**
     * Creates a calendar event and returns the event ID, or empty on failure.
     */
    public Optional<String> createEvent(String calendarId, String title, LocalDateTime startDateTime, LocalDateTime endDateTime, String description) {
        if (!isAvailable()) return Optional.empty();
        String calId = calendarId != null && !calendarId.isBlank() ? calendarId : defaultCalendarId;
        try {
            Event event = new Event();
            event.setSummary(title);
            if (description != null && !description.isBlank()) {
                event.setDescription(description);
            }
            event.setStart(new EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(startDateTime.atZone(ZoneId.systemDefault()).toInstant().toString()))
                    .setTimeZone(ZoneId.systemDefault().getId()));
            event.setEnd(new EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(endDateTime.atZone(ZoneId.systemDefault()).toInstant().toString()))
                    .setTimeZone(ZoneId.systemDefault().getId()));
            Event created = calendar.events().insert(calId, event).execute();
            return Optional.ofNullable(created.getId());
        } catch (IOException e) {
            log.warn("Google Calendar createEvent failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Deletes a calendar event by ID. Logs errors without throwing.
     */
    @Async
    public void deleteEventAsync(String calendarId, String eventId) {
        if (!isAvailable() || eventId == null || eventId.isBlank()) return;
        try {
            deleteEvent(calendarId != null && !calendarId.isBlank() ? calendarId : defaultCalendarId, eventId);
        } catch (Exception e) {
            log.warn("Failed to delete Google Calendar event {}: {}", eventId, e.getMessage());
        }
    }

    public boolean deleteEvent(String calendarId, String eventId) {
        if (!isAvailable() || eventId == null || eventId.isBlank()) return false;
        String calId = calendarId != null && !calendarId.isBlank() ? calendarId : defaultCalendarId;
        try {
            calendar.events().delete(calId, eventId).execute();
            return true;
        } catch (IOException e) {
            log.warn("Google Calendar deleteEvent failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Lists events in the given time range.
     */
    public List<Event> listEvents(String calendarId, LocalDateTime timeMin, LocalDateTime timeMax) {
        if (!isAvailable()) return Collections.emptyList();
        String calId = calendarId != null && !calendarId.isBlank() ? calendarId : defaultCalendarId;
        try {
            String tMin = timeMin.atZone(ZoneId.systemDefault()).toInstant().toString();
            String tMax = timeMax.atZone(ZoneId.systemDefault()).toInstant().toString();
            Events events = calendar.events().list(calId)
                    .setTimeMin(new com.google.api.client.util.DateTime(tMin))
                    .setTimeMax(new com.google.api.client.util.DateTime(tMax))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            return events.getItems() != null ? events.getItems() : Collections.emptyList();
        } catch (IOException e) {
            log.warn("Google Calendar listEvents failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
