package com.esprit.planning.service;

import com.esprit.planning.client.ProjectClient;
import com.esprit.planning.dto.CalendarEventDto;
import com.esprit.planning.dto.ProjectDto;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.repository.ProgressUpdateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds calendar events from Planning DB (progress updates with next due, project deadlines)
 * when Google Calendar is disabled or to supplement it. Supports filtering by userId so clients
 * see only their projects and freelancers see only their assigned work.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CalendarEventService {

    private final ProgressUpdateRepository progressUpdateRepository;
    private final ProjectClient projectClient;

    /**
     * Returns calendar events from our own data (no user filter). Use {@link #listEventsFromDb(LocalDateTime, LocalDateTime, Long, String)}
     * to restrict to a specific user.
     */
    public List<CalendarEventDto> listEventsFromDb(LocalDateTime timeMin, LocalDateTime timeMax) {
        return listEventsFromDb(timeMin, timeMax, null, null);
    }

    /**
     * Returns calendar events from our own data, optionally filtered by the given user.
     * When userId is non-null: progress "next due" events are included only if the user is the
     * freelancer who created the update or the client who owns the project; project deadlines
     * are included only if the user is the project client or a freelancer with progress updates on that project.
     */
    public List<CalendarEventDto> listEventsFromDb(LocalDateTime timeMin, LocalDateTime timeMax, Long userId, String role) {
        List<CalendarEventDto> events = new ArrayList<>();
        boolean filterByUser = userId != null;

        // Progress updates with "next update due" in range
        try {
            List<ProgressUpdate> updates = progressUpdateRepository.findByNextUpdateDueBetween(timeMin, timeMax);
            java.util.Map<Long, Long> projectIdToClientId = filterByUser ? new java.util.HashMap<>() : null;
            for (ProgressUpdate pu : updates) {
                if (filterByUser && !isProgressUpdateVisibleToUser(pu, userId, projectIdToClientId)) continue;
                LocalDateTime start = pu.getNextUpdateDue();
                if (start == null) continue;
                String summary = "Next progress update due – " + (pu.getTitle() != null ? pu.getTitle() : "Update #" + pu.getId());
                events.add(CalendarEventDto.builder()
                        .id("pu-" + pu.getId())
                        .summary(summary)
                        .start(start)
                        .end(start.plus(1, ChronoUnit.HOURS))
                        .description("Progress update #" + pu.getId() + " – Project " + pu.getProjectId())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to load progress-update calendar events: {}", e.getMessage());
        }

        // Project deadlines in range (from Project microservice)
        try {
            List<ProjectDto> projects = projectClient.getProjects();
            Set<Long> freelancerProjectIds = filterByUser ? new HashSet<>(progressUpdateRepository.findDistinctProjectIdsByFreelancerId(userId)) : null;
            if (projects != null) {
                for (ProjectDto p : projects) {
                    if (filterByUser && !isProjectDeadlineVisibleToUser(p, userId, freelancerProjectIds)) continue;
                    LocalDateTime deadline = p.getDeadline();
                    if (deadline == null) continue;
                    if (deadline.isBefore(timeMin) || deadline.isAfter(timeMax)) continue;
                    String title = p.getTitle() != null && !p.getTitle().isBlank() ? p.getTitle() : "Project #" + p.getId();
                    events.add(CalendarEventDto.builder()
                            .id("proj-" + p.getId())
                            .summary("Project deadline – " + title)
                            .start(deadline)
                            .end(deadline.plus(1, ChronoUnit.HOURS))
                            .description("Project #" + p.getId() + " deadline.")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load project-deadline calendar events (Project service may be down): {}", e.getMessage());
        }

        events.sort(Comparator.comparing(CalendarEventDto::getStart, Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    private boolean isProgressUpdateVisibleToUser(ProgressUpdate pu, Long userId, java.util.Map<Long, Long> projectIdToClientId) {
        if (pu.getFreelancerId() != null && pu.getFreelancerId().equals(userId)) return true;
        Long projectId = pu.getProjectId();
        if (projectId == null) return false;
        Long clientId = projectIdToClientId.computeIfAbsent(projectId, id -> {
            try {
                ProjectDto proj = projectClient.getProjectById(id);
                return proj != null ? proj.getClientId() : null;
            } catch (Exception e) {
                return null;
            }
        });
        return clientId != null && clientId.equals(userId);
    }

    private boolean isProjectDeadlineVisibleToUser(ProjectDto p, Long userId, Set<Long> freelancerProjectIds) {
        if (p.getClientId() != null && p.getClientId().equals(userId)) return true;
        if (p.getId() != null && freelancerProjectIds != null && freelancerProjectIds.contains(p.getId())) return true;
        return false;
    }
}
