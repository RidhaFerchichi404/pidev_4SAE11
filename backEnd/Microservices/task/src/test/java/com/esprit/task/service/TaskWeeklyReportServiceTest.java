package com.esprit.task.service;

import com.esprit.task.dto.TaskPriorityCountDto;
import com.esprit.task.dto.TaskStatsExtendedDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskWeeklyReportServiceTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskWeeklyReportService taskWeeklyReportService;

    @Test
    void normalizeWeekStartMonday_wednesdayAlignsToMondayOfSameWeek() {
        LocalDate wed = LocalDate.of(2026, 4, 8);
        assertThat(TaskWeeklyReportService.normalizeWeekStartMonday(wed)).isEqualTo(LocalDate.of(2026, 4, 6));
        assertThat(TaskWeeklyReportService.normalizeWeekStartMonday(wed).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void normalizeWeekStartMonday_null_returnsMondayOfCurrentWeek() {
        assertThat(TaskWeeklyReportService.normalizeWeekStartMonday(null).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void buildWeeklyPdf_returnsNonEmptyPdf() {
        LocalDate mon = LocalDate.of(2026, 4, 6);
        LocalDate sun = mon.plusDays(6);
        TaskStatsExtendedDto dto = TaskStatsExtendedDto.builder()
                .totalTasks(1)
                .doneCount(0)
                .inProgressCount(0)
                .inReviewCount(0)
                .todoCount(1)
                .cancelledCount(0)
                .overdueCount(0)
                .completionPercentage(0)
                .unassignedCount(0)
                .createdInRangeCount(0)
                .completedInRangeCount(0)
                .priorityBreakdown(List.of(
                        TaskPriorityCountDto.builder().priority(com.esprit.task.entity.TaskPriority.MEDIUM).count(1L).build()))
                .build();
        when(taskService.getExtendedStatsForWeeklyWindow(any(), any(), eq(mon), eq(sun)))
                .thenReturn(dto);
        when(taskService.getHighPriorityOpenLinesForReport(any(), any(), eq(20)))
                .thenReturn(List.of());

        byte[] pdf = taskWeeklyReportService.buildWeeklyPdf(Optional.empty(), Optional.empty(), mon);

        assertThat(pdf).hasSizeGreaterThan(200);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void buildRollingPeriodPdf_returnsNonEmptyPdf() {
        LocalDate end = LocalDate.of(2026, 4, 10);
        LocalDate start = end.minusDays(6);
        TaskStatsExtendedDto dto = TaskStatsExtendedDto.builder()
                .totalTasks(2)
                .doneCount(1)
                .inProgressCount(0)
                .inReviewCount(0)
                .todoCount(1)
                .cancelledCount(0)
                .overdueCount(0)
                .completionPercentage(50)
                .unassignedCount(0)
                .createdInRangeCount(0)
                .completedInRangeCount(0)
                .priorityBreakdown(List.of())
                .build();
        when(taskService.getExtendedStatsForWeeklyWindow(any(), any(), eq(start), eq(end))).thenReturn(dto);
        when(taskService.getHighPriorityOpenLinesForReport(any(), any(), eq(20))).thenReturn(List.of());

        byte[] pdf = taskWeeklyReportService.buildRollingPeriodPdf(Optional.of(1L), Optional.empty(), end, 7);

        assertThat(pdf).hasSizeGreaterThan(200);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
