package com.esprit.planning.service;

import com.esprit.planning.client.ProjectClient;
import com.esprit.planning.dto.*;
import com.esprit.planning.dto.ProgressUpdateRequest;
import com.esprit.planning.dto.ProgressUpdateValidationResponse;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.entity.ProjectDeadlineSync;
import com.esprit.planning.exception.ProgressCannotDecreaseException;
import com.esprit.planning.repository.ProgressCommentRepository;
import com.esprit.planning.repository.ProgressUpdateRepository;
import com.esprit.planning.repository.ProgressUpdateSpecification;
import com.esprit.planning.repository.ProjectDeadlineSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressUpdateService {

    private final ProgressUpdateRepository progressUpdateRepository;
    private final ProgressCommentRepository progressCommentRepository;
    private final PlanningNotificationService planningNotificationService;
    private final ProjectClient projectClient;
    private final GoogleCalendarService googleCalendarService;
    private final ProjectDeadlineSyncRepository projectDeadlineSyncRepository;

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findAll() {
        return progressUpdateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<ProgressUpdate> findAllFiltered(
            Optional<Long> projectId,
            Optional<Long> freelancerId,
            Optional<Long> contractId,
            Optional<Integer> progressMin,
            Optional<Integer> progressMax,
            Optional<LocalDate> dateFrom,
            Optional<LocalDate> dateTo,
            Optional<String> search,
            Pageable pageable) {
        var spec = ProgressUpdateSpecification.filtered(
                projectId, freelancerId, contractId, progressMin, progressMax, dateFrom, dateTo, search);
        return progressUpdateRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public ProgressUpdate findById(Long id) {
        return progressUpdateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProgressUpdate not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByProjectId(Long projectId) {
        return progressUpdateRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByContractId(Long contractId) {
        return progressUpdateRepository.findByContractId(contractId);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByFreelancerId(Long freelancerId) {
        return progressUpdateRepository.findByFreelancerId(freelancerId);
    }

    @Transactional(readOnly = true)
    public Optional<ProgressUpdate> findLatestByProjectId(Long projectId) {
        return progressUpdateRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<ProgressUpdate> findLatestByFreelancerId(Long freelancerId) {
        return progressUpdateRepository.findFirstByFreelancerIdOrderByCreatedAtDesc(freelancerId);
    }

    @Transactional(readOnly = true)
    public Optional<ProgressUpdate> findLatestByContractId(Long contractId) {
        return progressUpdateRepository.findFirstByContractIdOrderByCreatedAtDesc(contractId);
    }

    /**
     * Returns the maximum progress percentage for the given project, from updates
     * that are not the one with excludeUpdateId (pass null to include all).
     */
    @Transactional(readOnly = true)
    public int getMaxProgressPercentageForProject(Long projectId, Long excludeUpdateId) {
        List<ProgressUpdate> updates = progressUpdateRepository.findByProjectId(projectId);
        return updates.stream()
                .filter(u -> excludeUpdateId == null || !u.getId().equals(excludeUpdateId))
                .mapToInt(ProgressUpdate::getProgressPercentage)
                .max()
                .orElse(0);
    }

    @Transactional
    public ProgressUpdate create(ProgressUpdate progressUpdate) {
        int minAllowed = getMaxProgressPercentageForProject(progressUpdate.getProjectId(), null);
        if (progressUpdate.getProgressPercentage() < minAllowed) {
            throw new ProgressCannotDecreaseException(minAllowed, progressUpdate.getProgressPercentage());
        }
        ProgressUpdate saved = progressUpdateRepository.save(progressUpdate);
        notifyClientAboutProgress(saved.getProjectId(), saved.getFreelancerId(), "New progress update", saved.getTitle(),
            PlanningNotificationService.TYPE_PROGRESS_UPDATE, saved.getId(), saved.getProgressPercentage());
        syncNextDueCalendarEvent(saved);
        if (saved.getProgressPercentage() != null && saved.getProgressPercentage() == 100) {
            googleCalendarService.createEventAsync(null,
                "Milestone: 100% – " + saved.getTitle(),
                saved.getCreatedAt(), saved.getCreatedAt().plusHours(1),
                "Progress update #" + saved.getId() + " reached 100%.");
            notifyFreelancerCalendar(saved.getFreelancerId(), "Milestone reached", "100% completed: " + saved.getTitle(),
                PlanningNotificationService.TYPE_CALENDAR_MILESTONE, saved.getProjectId(), saved.getId());
        }
        ensureProjectDeadlineInCalendar(saved.getProjectId(), saved.getFreelancerId());
        return saved;
    }

    @Transactional
    public ProgressUpdate update(Long id, ProgressUpdate updated) {
        ProgressUpdate existing = findById(id);
        int minAllowed = getMaxProgressPercentageForProject(updated.getProjectId(), id);
        if (updated.getProgressPercentage() < minAllowed) {
            throw new ProgressCannotDecreaseException(minAllowed, updated.getProgressPercentage());
        }
        LocalDateTime previousNextDue = existing.getNextUpdateDue();
        String previousEventId = existing.getNextDueCalendarEventId();
        existing.setProjectId(updated.getProjectId());
        existing.setContractId(updated.getContractId());
        existing.setFreelancerId(updated.getFreelancerId());
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setProgressPercentage(updated.getProgressPercentage());
        existing.setNextUpdateDue(updated.getNextUpdateDue());
        ProgressUpdate saved = progressUpdateRepository.save(existing);
        notifyClientAboutProgress(saved.getProjectId(), saved.getFreelancerId(), "Progress update edited", saved.getTitle(),
            PlanningNotificationService.TYPE_PROGRESS_UPDATE, saved.getId(), saved.getProgressPercentage());
        syncNextDueCalendarEventOnUpdate(saved, previousNextDue, previousEventId);
        if (saved.getProgressPercentage() != null && saved.getProgressPercentage() == 100) {
            googleCalendarService.createEventAsync(null,
                "Milestone: 100% – " + saved.getTitle(),
                saved.getUpdatedAt(), saved.getUpdatedAt().plusHours(1),
                "Progress update #" + saved.getId() + " reached 100%.");
            notifyFreelancerCalendar(saved.getFreelancerId(), "Milestone reached", "100% completed: " + saved.getTitle(),
                PlanningNotificationService.TYPE_CALENDAR_MILESTONE, saved.getProjectId(), saved.getId());
        }
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        ProgressUpdate existing = findById(id);
        Long projectId = existing.getProjectId();
        Long freelancerId = existing.getFreelancerId();
        String title = existing.getTitle();
        String calendarEventId = existing.getNextDueCalendarEventId();
        progressUpdateRepository.deleteById(id);
        if (calendarEventId != null && !calendarEventId.isBlank()) {
            googleCalendarService.deleteEventAsync(null, calendarEventId);
        }
        notifyClientAboutProgress(projectId, freelancerId, "Progress update removed", title,
            PlanningNotificationService.TYPE_PROGRESS_UPDATE, id, null);
    }

    /** Creates the "next progress update due" calendar event after create. */
    private void syncNextDueCalendarEvent(ProgressUpdate saved) {
        LocalDateTime nextDue = saved.getNextUpdateDue();
        if (nextDue == null) return;
        String title = "Next progress update due – " + saved.getTitle();
        java.util.Optional<String> eventId = googleCalendarService.createEvent(null, title,
                nextDue, nextDue.plus(1, ChronoUnit.HOURS),
                "Progress update #" + saved.getId() + " – Project " + saved.getProjectId());
        eventId.ifPresent(id -> {
            saved.setNextDueCalendarEventId(id);
            progressUpdateRepository.save(saved);
            notifyFreelancerCalendar(saved.getFreelancerId(), "Calendar reminder", "Next progress update due: " + saved.getTitle(),
                PlanningNotificationService.TYPE_CALENDAR_REMINDER, saved.getProjectId(), saved.getId());
        });
    }

    /** Updates or deletes the "next progress update due" calendar event after update. */
    private void syncNextDueCalendarEventOnUpdate(ProgressUpdate saved, LocalDateTime previousNextDue, String previousEventId) {
        if (previousEventId != null && !previousEventId.isBlank()
                && (previousNextDue == null || !previousNextDue.equals(saved.getNextUpdateDue()))) {
            googleCalendarService.deleteEventAsync(null, previousEventId);
            saved.setNextDueCalendarEventId(null);
        }
        if (saved.getNextUpdateDue() != null) {
            String title = "Next progress update due – " + saved.getTitle();
            java.util.Optional<String> eventId = googleCalendarService.createEvent(null, title,
                    saved.getNextUpdateDue(), saved.getNextUpdateDue().plus(1, ChronoUnit.HOURS),
                    "Progress update #" + saved.getId() + " – Project " + saved.getProjectId());
            eventId.ifPresent(id -> {
                saved.setNextDueCalendarEventId(id);
                progressUpdateRepository.save(saved);
                notifyFreelancerCalendar(saved.getFreelancerId(), "Calendar reminder", "Next progress update due: " + saved.getTitle(),
                    PlanningNotificationService.TYPE_CALENDAR_REMINDER, saved.getProjectId(), saved.getId());
            });
        } else {
            saved.setNextDueCalendarEventId(null);
        }
    }

    /**
     * Notify the freelancer about a calendar-related event (reminder, milestone, deadline added).
     */
    private void notifyFreelancerCalendar(Long freelancerId, String title, String body, String type, Long projectId, Long progressUpdateId) {
        if (freelancerId == null) return;
        java.util.Map<String, String> data = new java.util.HashMap<>();
        if (projectId != null) data.put("projectId", String.valueOf(projectId));
        if (progressUpdateId != null) data.put("progressUpdateId", String.valueOf(progressUpdateId));
        planningNotificationService.notifyUser(String.valueOf(freelancerId), title, body, type, data);
    }

    /**
     * Public entry point: ensure project deadline is in the calendar for the given freelancer (e.g. when they open the project).
     * Idempotent: no duplicate events created.
     */
    @Transactional
    public void ensureProjectDeadlineInCalendarForUser(Long projectId, Long freelancerId) {
        ensureProjectDeadlineInCalendar(projectId, freelancerId);
    }

    /**
     * If the project has a deadline and it is not yet in the calendar, add it and notify the freelancer.
     */
    private void ensureProjectDeadlineInCalendar(Long projectId, Long freelancerId) {
        if (projectId == null || freelancerId == null) return;
        if (projectDeadlineSyncRepository.findByProjectId(projectId).isPresent()) return;
        try {
            var project = projectClient.getProjectById(projectId);
            if (project == null || project.getDeadline() == null) return;
            String title = "Project deadline – " + (project.getTitle() != null ? project.getTitle() : "Project #" + projectId);
            LocalDateTime deadline = project.getDeadline();
            Optional<String> eventId = googleCalendarService.createEvent(null, title,
                    deadline, deadline.plus(1, ChronoUnit.HOURS),
                    "Project #" + projectId + " deadline.");
            eventId.ifPresent(id -> {
                projectDeadlineSyncRepository.save(ProjectDeadlineSync.builder()
                        .projectId(projectId)
                        .calendarEventId(id)
                        .syncedAt(LocalDateTime.now())
                        .build());
                notifyFreelancerCalendar(freelancerId, "Project deadline in calendar",
                        "Deadline for \"" + title + "\" has been added to your calendar.",
                        PlanningNotificationService.TYPE_CALENDAR_DEADLINE, projectId, null);
            });
        } catch (Exception ignored) { /* project service may be down */ }
    }

    /**
     * Notify the project owner (client) about progress updates. Uses projectId to get clientId from Project service.
     * Never notifies the freelancer who created the update.
     */
    private void notifyClientAboutProgress(Long projectId, Long freelancerId, String notifTitle, String body,
                                           String type, Long progressUpdateId, Integer progressPercentage) {
        if (projectId == null) return;
        try {
            var project = projectClient.getProjectById(projectId);
            if (project == null || project.getClientId() == null) return;
            Long clientId = project.getClientId();
            if (freelancerId != null && freelancerId.equals(clientId)) return;
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("progressUpdateId", String.valueOf(progressUpdateId));
            data.put("projectId", String.valueOf(projectId));
            if (progressPercentage != null) data.put("progressPercentage", String.valueOf(progressPercentage));
            planningNotificationService.notifyUser(
                String.valueOf(clientId),
                notifTitle,
                body != null ? body : "",
                type,
                data);
        } catch (Exception ignored) { /* project service may be down */ }
    }

    @Transactional(readOnly = true)
    public Integer getNextAllowedPercentageForProject(Long projectId) {
        return getMaxProgressPercentageForProject(projectId, null);
    }

    @Transactional(readOnly = true)
    public ProgressUpdateValidationResponse validate(ProgressUpdateRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getProjectId() == null) {
            errors.add("projectId is required");
        }
        if (request.getFreelancerId() == null) {
            errors.add("freelancerId is required");
        }
        Integer provided = request.getProgressPercentage();
        if (provided == null) {
            errors.add("progressPercentage is required");
        } else if (provided < 0 || provided > 100) {
            errors.add("progressPercentage must be between 0 and 100");
        }

        Integer minAllowed = null;
        if (request.getProjectId() != null) {
            minAllowed = getNextAllowedPercentageForProject(request.getProjectId());
            if (provided != null && provided < minAllowed) {
                errors.add("progressPercentage cannot be less than previously recorded progress for this project");
            }
        }

        boolean valid = errors.isEmpty();
        return ProgressUpdateValidationResponse.builder()
                .valid(valid)
                .minAllowed(minAllowed)
                .provided(provided)
                .errors(errors)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProgressTrendPointDto> getProgressTrendByProject(Long projectId, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay(); // exclusive end
        List<ProgressUpdate> updates = progressUpdateRepository.findByProjectIdAndCreatedAtBetween(projectId, fromDateTime, toDateTime);
        return updates.stream()
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().toLocalDate()))
                .entrySet().stream()
                .map(e -> ProgressTrendPointDto.builder()
                        .date(e.getKey())
                        .progressPercentage(
                                e.getValue().stream()
                                        .max(Comparator.comparing(ProgressUpdate::getCreatedAt))
                                        .map(ProgressUpdate::getProgressPercentage)
                                        .orElse(0))
                        .build())
                .sorted(Comparator.comparing(ProgressTrendPointDto::getDate))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgressReportDto getProgressReportForProject(Long projectId, LocalDate from, LocalDate to) {
        LocalDate fromEffective = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate toEffective = to != null ? to : LocalDate.now();

        LocalDateTime fromDateTime = fromEffective.atStartOfDay();
        LocalDateTime toDateTime = toEffective.plusDays(1).atStartOfDay(); // exclusive end

        List<ProgressUpdate> updates = progressUpdateRepository.findByProjectIdAndCreatedAtBetween(projectId, fromDateTime, toDateTime);
        List<Long> updateIds = updates.stream().map(ProgressUpdate::getId).collect(Collectors.toList());

        long commentCount = updateIds.isEmpty() ? 0 : progressCommentRepository.countByProgressUpdate_IdIn(updateIds);

        Double avgPct = updates.isEmpty() ? null : updates.stream()
                .mapToInt(ProgressUpdate::getProgressPercentage)
                .average()
                .orElse(0.0);

        LocalDateTime firstUpdateAt = updates.stream()
                .map(ProgressUpdate::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime lastUpdateAt = updates.stream()
                .map(ProgressUpdate::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return ProgressReportDto.builder()
                .projectId(projectId)
                .from(fromEffective)
                .to(toEffective)
                .updateCount(updates.size())
                .commentCount(commentCount)
                .averageProgressPercentage(updates.isEmpty() ? null : avgPct)
                .firstUpdateAt(firstUpdateAt)
                .lastUpdateAt(lastUpdateAt)
                .build();
    }

    // --- Statistics ---

    @Transactional(readOnly = true)
    public FreelancerProgressStatsDto getProgressStatisticsByFreelancer(Long freelancerId) {
        List<ProgressUpdate> updates = progressUpdateRepository.findByFreelancerId(freelancerId);
        List<Long> updateIds = updates.stream().map(ProgressUpdate::getId).collect(Collectors.toList());
        long totalComments = updateIds.isEmpty() ? 0 : progressCommentRepository.countByProgressUpdate_IdIn(updateIds);

        double avgPct = updates.isEmpty() ? 0.0 : updates.stream()
                .mapToInt(ProgressUpdate::getProgressPercentage)
                .average()
                .orElse(0.0);

        LocalDateTime lastUpdateAt = updates.stream()
                .map(ProgressUpdate::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long updatesLast30Days = updates.stream()
                .filter(u -> u.getUpdatedAt() != null && !u.getUpdatedAt().isBefore(thirtyDaysAgo))
                .count();

        return FreelancerProgressStatsDto.builder()
                .freelancerId(freelancerId)
                .totalUpdates(updates.size())
                .totalComments(totalComments)
                .averageProgressPercentage(updates.isEmpty() ? null : avgPct)
                .lastUpdateAt(lastUpdateAt)
                .updatesLast30Days(updatesLast30Days)
                .build();
    }

    @Transactional(readOnly = true)
    public ProjectProgressStatsDto getProgressStatisticsByProject(Long projectId) {
        List<ProgressUpdate> updates = progressUpdateRepository.findByProjectId(projectId);
        List<Long> updateIds = updates.stream().map(ProgressUpdate::getId).collect(Collectors.toList());
        long commentCount = updateIds.isEmpty() ? 0 : progressCommentRepository.countByProgressUpdate_IdIn(updateIds);

        Integer currentProgressPercentage = updates.stream()
                .max(Comparator.nullsLast(Comparator.comparing(ProgressUpdate::getUpdatedAt)))
                .map(ProgressUpdate::getProgressPercentage)
                .orElse(null);

        LocalDateTime firstUpdateAt = updates.stream()
                .map(ProgressUpdate::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime lastUpdateAt = updates.stream()
                .map(ProgressUpdate::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return ProjectProgressStatsDto.builder()
                .projectId(projectId)
                .updateCount(updates.size())
                .commentCount(commentCount)
                .currentProgressPercentage(currentProgressPercentage)
                .firstUpdateAt(firstUpdateAt)
                .lastUpdateAt(lastUpdateAt)
                .build();
    }

    @Transactional(readOnly = true)
    public ContractProgressStatsDto getProgressStatisticsByContract(Long contractId) {
        List<ProgressUpdate> updates = progressUpdateRepository.findByContractId(contractId);
        List<Long> updateIds = updates.stream().map(ProgressUpdate::getId).collect(Collectors.toList());
        long commentCount = updateIds.isEmpty() ? 0 : progressCommentRepository.countByProgressUpdate_IdIn(updateIds);

        Integer currentProgressPercentage = updates.stream()
                .max(Comparator.nullsLast(Comparator.comparing(ProgressUpdate::getUpdatedAt)))
                .map(ProgressUpdate::getProgressPercentage)
                .orElse(null);

        LocalDateTime firstUpdateAt = updates.stream()
                .map(ProgressUpdate::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime lastUpdateAt = updates.stream()
                .map(ProgressUpdate::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return ContractProgressStatsDto.builder()
                .contractId(contractId)
                .updateCount(updates.size())
                .commentCount(commentCount)
                .currentProgressPercentage(currentProgressPercentage)
                .firstUpdateAt(firstUpdateAt)
                .lastUpdateAt(lastUpdateAt)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStatistics() {
        List<ProgressUpdate> all = progressUpdateRepository.findAll();
        List<Long> updateIds = all.stream().map(ProgressUpdate::getId).collect(Collectors.toList());
        long totalComments = updateIds.isEmpty() ? 0 : progressCommentRepository.countByProgressUpdate_IdIn(updateIds);

        double avgPct = all.isEmpty() ? 0.0 : all.stream()
                .mapToInt(ProgressUpdate::getProgressPercentage)
                .average()
                .orElse(0.0);

        long distinctProjectCount = all.stream().map(ProgressUpdate::getProjectId).distinct().count();
        long distinctFreelancerCount = all.stream().map(ProgressUpdate::getFreelancerId).distinct().count();

        return DashboardStatsDto.builder()
                .totalUpdates(all.size())
                .totalComments(totalComments)
                .averageProgressPercentage(all.isEmpty() ? null : avgPct)
                .distinctProjectCount(distinctProjectCount)
                .distinctFreelancerCount(distinctFreelancerCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findAllFilteredForExport(
            Optional<Long> projectId,
            Optional<Long> freelancerId,
            Optional<Long> contractId,
            Optional<Integer> progressMin,
            Optional<Integer> progressMax,
            Optional<LocalDate> dateFrom,
            Optional<LocalDate> dateTo,
            Optional<String> search) {
        var spec = ProgressUpdateSpecification.filtered(
                projectId, freelancerId, contractId, progressMin, progressMax, dateFrom, dateTo, search);
        return progressUpdateRepository.findAll(spec, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    // --- Stalled projects (section 4) ---

    @Transactional(readOnly = true)
    public List<StalledProjectDto> getProjectIdsWithStalledProgress(int daysWithoutUpdate) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysWithoutUpdate);
        List<Object[]> projectIdAndMaxUpdatedAt = progressUpdateRepository.findProjectIdAndMaxUpdatedAt();
        return projectIdAndMaxUpdatedAt.stream()
                .filter(row -> {
                    LocalDateTime lastUpdateAt = (LocalDateTime) row[1];
                    return lastUpdateAt != null && lastUpdateAt.isBefore(cutoff);
                })
                .map(row -> {
                    Long projectId = (Long) row[0];
                    LocalDateTime lastUpdateAt = (LocalDateTime) row[1];
                    Integer lastProgressPercentage = progressUpdateRepository
                            .findByProjectIdAndUpdatedAt(projectId, lastUpdateAt)
                            .map(ProgressUpdate::getProgressPercentage)
                            .orElse(null);
                    return new StalledProjectDto(projectId, lastUpdateAt, lastProgressPercentage);
                })
                .collect(Collectors.toList());
    }

    // --- Rankings (section 5) ---

    @Transactional(readOnly = true)
    public List<FreelancerActivityDto> getFreelancersByActivity(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        List<Object[]> rows = progressUpdateRepository.findFreelancerIdAndUpdateCountOrderByCountDesc(pageable);
        return rows.stream()
                .map(row -> {
                    Long freelancerId = (Long) row[0];
                    long updateCount = (Long) row[1];
                    long commentCount = progressCommentRepository.countByProgressUpdate_FreelancerId(freelancerId);
                    return new FreelancerActivityDto(freelancerId, updateCount, commentCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectActivityDto> getMostActiveProjects(int limit, Optional<LocalDate> from, Optional<LocalDate> to) {
        LocalDateTime fromDateTime = from.map(d -> d.atStartOfDay()).orElse(null);
        LocalDateTime toDateTime = to.map(d -> d.plusDays(1).atStartOfDay()).orElse(null); // end of day inclusive via < toDateTime
        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        List<Object[]> rows = progressUpdateRepository.findProjectIdAndUpdateCountOrderByCountDescBetween(
                fromDateTime, toDateTime, pageable);
        return rows.stream()
                .map(row -> new ProjectActivityDto((Long) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }
}
