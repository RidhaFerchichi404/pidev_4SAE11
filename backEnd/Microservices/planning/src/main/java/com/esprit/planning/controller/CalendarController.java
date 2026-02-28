package com.esprit.planning.controller;

import com.esprit.planning.dto.CalendarEventDto;
import com.esprit.planning.service.CalendarEventService;
import com.esprit.planning.service.GoogleCalendarService;
import com.esprit.planning.service.ProgressUpdateService;
import com.google.api.services.calendar.model.Event;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Calendar", description = "Calendar events (Google Calendar when enabled, else from progress updates and project deadlines)")
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final ProgressUpdateService progressUpdateService;
    private final CalendarEventService calendarEventService;

    @PostMapping("/sync-project-deadline")
    @Operation(
            summary = "Sync project deadline to calendar",
            description = "Ensures the project deadline is added to the calendar for the given freelancer. Idempotent. Notifies the freelancer when the deadline is first added."
    )
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<Void> syncProjectDeadline(
            @Parameter(description = "Project ID", required = true) @RequestParam Long projectId,
            @Parameter(description = "Freelancer user ID (to notify)", required = true) @RequestParam Long freelancerId) {
        progressUpdateService.ensureProjectDeadlineInCalendarForUser(projectId, freelancerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/events")
    @Operation(
            summary = "List calendar events",
            description = "Returns events in the given time range, optionally scoped to a user. When userId is provided, only events relevant to that client or freelancer are returned. When Google Calendar is enabled and no userId is passed, events come from Google; otherwise from progress updates (next due) and project deadlines. timeMin/timeMax accept ISO-8601 (e.g. with Z or offset)."
    )
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<CalendarEventDto>> listEvents(
            @Parameter(description = "Start of range (ISO-8601 date-time, e.g. 2026-02-28T03:19:42.848Z)")
            @RequestParam(required = false) String timeMin,
            @Parameter(description = "End of range (ISO-8601 date-time)")
            @RequestParam(required = false) String timeMax,
            @Parameter(description = "Calendar ID (default from config, used only when Google Calendar is enabled and no userId)")
            @RequestParam(required = false) String calendarId,
            @Parameter(description = "Current user ID – when set, only events for this client/freelancer are returned")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Current user role (CLIENT, FREELANCER, ADMIN) – used with userId for filtering")
            @RequestParam(required = false) String role) {
        LocalDateTime min = parseDateTime(timeMin, LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
        LocalDateTime max = parseDateTime(timeMax, min.plusMonths(2));
        List<CalendarEventDto> dtos;
        boolean scopeToUser = userId != null && !"ADMIN".equalsIgnoreCase(role);
        if (scopeToUser) {
            dtos = calendarEventService.listEventsFromDb(min, max, userId, role);
        } else if (userId == null && googleCalendarService.isAvailable()) {
            List<Event> events = googleCalendarService.listEvents(calendarId, min, max);
            dtos = events.stream().map(this::toDto).collect(Collectors.toList());
        } else {
            dtos = calendarEventService.listEventsFromDb(min, max);
        }
        return ResponseEntity.ok(dtos);
    }

    /** Parse ISO-8601 string (with Z or offset) to LocalDateTime in system default zone; on failure return defaultValue. */
    private static LocalDateTime parseDateTime(String value, LocalDateTime defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private CalendarEventDto toDto(Event e) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (e.getStart() != null && e.getStart().getDateTime() != null) {
            start = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(e.getStart().getDateTime().getValue()),
                    ZoneId.systemDefault());
        }
        if (e.getEnd() != null && e.getEnd().getDateTime() != null) {
            end = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(e.getEnd().getDateTime().getValue()),
                    ZoneId.systemDefault());
        }
        return CalendarEventDto.builder()
                .id(e.getId())
                .summary(e.getSummary())
                .start(start)
                .end(end)
                .description(e.getDescription())
                .build();
    }
}
