package tn.esprit.freelanciajob.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import tn.esprit.freelanciajob.Controller.JobStatsController;
import tn.esprit.freelanciajob.Dto.response.JobStatsDTO;
import tn.esprit.freelanciajob.Service.JobStatsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobStatsControllerTest {

    @Mock
    private JobStatsService statsService;

    @InjectMocks
    private JobStatsController controller;

    @Test
    void getJobStats_returnsServicePayload() {
        JobStatsDTO dto = JobStatsDTO.builder().totalJobs(22L).build();
        when(statsService.getStats()).thenReturn(dto);

        ResponseEntity<JobStatsDTO> response = controller.getJobStats();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalJobs()).isEqualTo(22L);
    }
}
