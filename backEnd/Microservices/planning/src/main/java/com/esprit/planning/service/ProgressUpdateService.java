package com.esprit.planning.service;

import com.esprit.planning.dto.*;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.exception.ProgressCannotDecreaseException;
import com.esprit.planning.repository.ProgressCommentRepository;
import com.esprit.planning.repository.ProgressUpdateRepository;
import com.esprit.planning.repository.ProgressUpdateSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressUpdateService {

    private final ProgressUpdateRepository progressUpdateRepository;
    private final ProgressCommentRepository progressCommentRepository;

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
        return progressUpdateRepository.save(progressUpdate);
    }

    @Transactional
    public ProgressUpdate update(Long id, ProgressUpdate updated) {
        ProgressUpdate existing = findById(id);
        int minAllowed = getMaxProgressPercentageForProject(updated.getProjectId(), id);
        if (updated.getProgressPercentage() < minAllowed) {
            throw new ProgressCannotDecreaseException(minAllowed, updated.getProgressPercentage());
        }
        existing.setProjectId(updated.getProjectId());
        existing.setContractId(updated.getContractId());
        existing.setFreelancerId(updated.getFreelancerId());
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setProgressPercentage(updated.getProgressPercentage());
        return progressUpdateRepository.save(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!progressUpdateRepository.existsById(id)) {
            throw new RuntimeException("ProgressUpdate not found with id: " + id);
        }
        progressUpdateRepository.deleteById(id);
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
