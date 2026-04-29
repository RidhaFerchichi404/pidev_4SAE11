package tn.esprit.freelanciajob.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import tn.esprit.freelanciajob.Controller.JobApplicationController;
import tn.esprit.freelanciajob.Dto.request.JobApplicationRequest;
import tn.esprit.freelanciajob.Dto.response.ApplyJobResponse;
import tn.esprit.freelanciajob.Dto.response.JobApplicationResponse;
import tn.esprit.freelanciajob.Entity.Enums.ApplicationStatus;
import tn.esprit.freelanciajob.Repository.ApplicationAttachmentRepository;
import tn.esprit.freelanciajob.Service.IJobApplicationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationControllerTest {

    @Mock
    private IJobApplicationService applicationService;
    @Mock
    private ApplicationAttachmentRepository attachmentRepository;
    @InjectMocks
    private JobApplicationController controller;

    @Test
    void crudEndpointsDelegateToService() {
        JobApplicationResponse response = new JobApplicationResponse();
        when(applicationService.addApplication(any(JobApplicationRequest.class))).thenReturn(response);
        when(applicationService.updateApplication(eq(1L), any(JobApplicationRequest.class))).thenReturn(response);
        when(applicationService.getApplicationById(1L)).thenReturn(response);
        when(applicationService.getAllApplications()).thenReturn(List.of(response));
        when(applicationService.getApplicationsByJob(2L)).thenReturn(List.of(response));
        when(applicationService.getApplicationsByFreelancer(3L)).thenReturn(List.of(response));
        when(applicationService.updateStatus(1L, ApplicationStatus.ACCEPTED)).thenReturn(response);

        assertThat(controller.addApplication(new JobApplicationRequest()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateApplication(1L, new JobApplicationRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteApplication(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.getApplicationById(1L).getBody()).isEqualTo(response);
        assertThat(controller.getAllApplications().getBody()).hasSize(1);
        assertThat(controller.getByJob(2L).getBody()).hasSize(1);
        assertThat(controller.getByFreelancer(3L).getBody()).hasSize(1);
        assertThat(controller.updateStatus(1L, "accepted").getBody()).isEqualTo(response);
    }

    @Test
    void applyToJobReturnsCreatedWithMultipartFlow() {
        ApplyJobResponse response = new ApplyJobResponse();
        when(applicationService.applyToJob(anyLong(), anyLong(), any(), any(), any(), any())).thenReturn(response);
        MockMultipartFile file = new MockMultipartFile("files", "cv.pdf", "application/pdf", "x".getBytes());

        var entity = controller.applyToJob(4L, 5L, "This is a sufficiently long proposal text", BigDecimal.TEN, LocalDate.now(), List.of(file));

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entity.getBody()).isEqualTo(response);
    }
}
