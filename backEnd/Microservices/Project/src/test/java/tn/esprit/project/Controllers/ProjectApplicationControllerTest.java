package tn.esprit.project.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.project.Dto.ProjectApplicationStats;
import tn.esprit.project.Entities.Enums.ApplicationStatus;
import tn.esprit.project.Entities.ProjectApplication;
import tn.esprit.project.Services.IProjectApplicationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = ProjectApplicationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
class ProjectApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IProjectApplicationService projectApplicationService;

    @Test
    void addApplication_nominalCase() throws Exception {
        ProjectApplication app = new ProjectApplication();
        app.setFreelanceId(1L);

        ProjectApplication saved = new ProjectApplication();
        saved.setId(10L);
        saved.setFreelanceId(1L);

        when(projectApplicationService.addProjectApplication(any(ProjectApplication.class))).thenReturn(saved);

        mockMvc.perform(post("/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(app)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(projectApplicationService).addProjectApplication(any(ProjectApplication.class));
    }

    @Test
    void updateApplication_nominalCase() throws Exception {
        ProjectApplication app = new ProjectApplication();
        app.setId(10L);

        when(projectApplicationService.updateProjectApplication(any(ProjectApplication.class))).thenReturn(app);

        mockMvc.perform(put("/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(app)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void updateStatus_nominalCase() throws Exception {
        ProjectApplication app = new ProjectApplication();
        app.setId(10L);
        app.setStatus(ApplicationStatus.ACCEPTED);

        when(projectApplicationService.updateStatus(10L, ApplicationStatus.ACCEPTED)).thenReturn(app);

        mockMvc.perform(put("/applications/{id}/status", 10L)
                .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void deleteApplication_nominalCase() throws Exception {
        mockMvc.perform(delete("/applications/{id}", 10L))
                .andExpect(status().isOk());

        verify(projectApplicationService).deleteProjectApplication(10L);
    }

    @Test
    void getApplicationById_nominalCase() throws Exception {
        ProjectApplication app = new ProjectApplication();
        app.setId(10L);

        when(projectApplicationService.getProjectApplicationById(10L)).thenReturn(app);

        mockMvc.perform(get("/applications/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void getApplicationsByProject_nominalCase() throws Exception {
        ProjectApplication app = new ProjectApplication();
        app.setId(10L);

        when(projectApplicationService.getApplicationsByProject(1L)).thenReturn(List.of(app));

        mockMvc.perform(get("/applications/project/{projectId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void getApplications_nominalCase() throws Exception {
        ProjectApplication app = new ProjectApplication();
        app.setId(10L);

        when(projectApplicationService.getAllProjectApplications()).thenReturn(List.of(app));

        mockMvc.perform(get("/applications/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void getApplicationsByFreelance_nominalCase() throws Exception {
        ProjectApplication app = new ProjectApplication();
        app.setId(10L);

        when(projectApplicationService.getApplicationsByFreelance(2L)).thenReturn(List.of(app));

        mockMvc.perform(get("/applications/freelance/{freelanceId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void getApplicationsStats_nominalCase() throws Exception {
        ProjectApplicationStats stats = new ProjectApplicationStats(1L, "Proj", 5L);

        when(projectApplicationService.getProjectApplicationStatistics()).thenReturn(List.of(stats));

        mockMvc.perform(get("/applications/applications/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1L))
                .andExpect(jsonPath("$[0].projectTitle").value("Proj"))
                .andExpect(jsonPath("$[0].applicationsCount").value(5));
    }
}
