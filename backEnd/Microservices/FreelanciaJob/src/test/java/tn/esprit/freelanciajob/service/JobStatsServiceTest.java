package tn.esprit.freelanciajob.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelanciajob.Dto.JobStats;
import tn.esprit.freelanciajob.Dto.projection.MonthlyJobProjection;
import tn.esprit.freelanciajob.Dto.projection.StatusCountProjection;
import tn.esprit.freelanciajob.Dto.response.JobStatsDTO;
import tn.esprit.freelanciajob.Repository.JobApplicationRepository;
import tn.esprit.freelanciajob.Repository.JobRepository;
import tn.esprit.freelanciajob.Service.JobStatsService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobStatsServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobApplicationRepository applicationRepository;

    @InjectMocks
    private JobStatsService service;

    @Test
    void getStats_aggregatesDashboardValues() {
        when(jobRepository.count()).thenReturn(4L);
        when(applicationRepository.count()).thenReturn(10L);
        when(applicationRepository.countUniqueFreelancers()).thenReturn(3L);
        when(jobRepository.countByStatus()).thenReturn(List.of(
                status("OPEN", 2L),
                status("CLOSED", 2L)
        ));
        when(jobRepository.getJobsStatistics()).thenReturn(List.of(
                new JobStats(1L, "A", 4L),
                new JobStats(2L, "B", 2L),
                new JobStats(3L, "C", 1L)
        ));
        when(jobRepository.getJobsPerMonth()).thenReturn(List.of(month("2026-03", 7L)));

        JobStatsDTO stats = service.getStats();

        assertThat(stats.getTotalJobs()).isEqualTo(4L);
        assertThat(stats.getAvgApplicationsPerJob()).isEqualTo(2.5);
        assertThat(stats.getUniqueFreelancers()).isEqualTo(3L);
        assertThat(stats.getJobsByStatus()).isEqualTo(Map.of("OPEN", 2L, "CLOSED", 2L));
        assertThat(stats.getTop5Jobs()).hasSize(3);
        assertThat(stats.getJobsPerMonth()).hasSize(1);
    }

    private StatusCountProjection status(String value, Long count) {
        return new StatusCountProjection() {
            @Override
            public String getStatus() { return value; }
            @Override
            public Long getCount() { return count; }
        };
    }

    private MonthlyJobProjection month(String m, Long c) {
        return new MonthlyJobProjection() {
            @Override
            public String getMonth() { return m; }
            @Override
            public Long getCount() { return c; }
        };
    }
}
