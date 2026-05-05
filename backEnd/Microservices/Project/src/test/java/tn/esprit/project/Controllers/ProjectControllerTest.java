package tn.esprit.project.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.project.Dto.request.ProjectRequest;
import tn.esprit.project.Dto.response.JointProjectItem;
import tn.esprit.project.Dto.response.JointProjectsResponse;
import tn.esprit.project.Dto.response.ProjectResponse;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Services.IProjectService;
import tn.esprit.project.Services.ProjectRiskService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProjectController.
 */
@WebMvcTest(
        value = ProjectController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@TestPropertySource(properties = "welcome.message=Welcome to Project Service")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IProjectService projectService;

    @MockitoBean
    private ProjectRiskService projectRiskService;

    @Test
    void welcome_returnsMessage() throws Exception {
        mockMvc.perform(get("/projects/welcome"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome to Project Service"));
    }

    @Test
    void addProject_nominalCase() throws Exception {
        ProjectRequest request = new ProjectRequest();
        request.setClientId(1L);
        request.setTitle("New Project");
        request.setDescription("Desc");
        request.setBudget(new BigDecimal("1000"));
        request.setDeadline(java.time.LocalDateTime.now().plusDays(10));
        request.setCategory("IT");
        request.setSkillIds(List.of(1L));

        Project p = new Project();
        p.setId(1L);
        p.setTitle("New Project");
        
        when(projectService.addProject(any(ProjectRequest.class))).thenReturn(p);

        mockMvc.perform(post("/projects/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New Project"));
    }

    @Test
    void updateProject_nominalCase() throws Exception {
        Project p = new Project();
        p.setId(1L);
        p.setTitle("Updated");

        when(projectService.updateProject(any(Project.class))).thenReturn(p);

        mockMvc.perform(put("/projects/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void deleteProject_nominalCase() throws Exception {
        mockMvc.perform(delete("/projects/1"))
                .andExpect(status().isOk());

        verify(projectService).deleteProject(1L);
    }

    @Test
    void getAllProjects_returnsArray() throws Exception {
        ProjectResponse p = new ProjectResponse();
        p.setId(1L);
        p.setTitle("P1");
        
        when(projectService.getAllProjectResponses()).thenReturn(List.of(p));

        mockMvc.perform(get("/projects/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("P1"));
    }

    @Test
    void getProjectsByClientId_returnsArray() throws Exception {
        Project p = new Project();
        p.setClientId(5L);
        
        when(projectService.getProjectsByClientId(5L)).thenReturn(List.of(p));

        mockMvc.perform(get("/projects/client/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].clientId").value(5));
    }

    @Test
    void getJointProjects_nominalCase() throws Exception {
        JointProjectsResponse jp = new JointProjectsResponse(1, List.of(JointProjectItem.builder().id(1L).build()));
        when(projectService.getJointProjects(1L, 2L)).thenReturn(jp);

        mockMvc.perform(get("/projects/joint")
                        .param("clientId", "1")
                        .param("freelancerId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharedProjectCount").value(1));
    }

    @Test
    void getProjectById_returnsRes() throws Exception {
        ProjectResponse p = new ProjectResponse();
        p.setId(1L);
        
        when(projectService.getProjectResponse(1L)).thenReturn(p);

        mockMvc.perform(get("/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getRecommendedProjects_returnsArray() throws Exception {
        ProjectResponse p = new ProjectResponse();
        p.setId(10L);
        
        when(projectService.getRecommendedProjects(5L)).thenReturn(List.of(p));

        mockMvc.perform(get("/projects/recommended")
                        .param("userId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void getStatistics_returnsMap() throws Exception {
        when(projectService.getProjectStatistics()).thenReturn(Map.of("totalProjects", 5L));

        mockMvc.perform(get("/projects/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(5));
    }

    @Test
    void exportProjectsPdf_returnsPdfBytes() throws Exception {
        when(projectService.exportProjectsToPdf()).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/projects/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=projects.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void countCompletedProjects_returnsLong() throws Exception {
        when(projectService.countCompletedProjectsByFreelancer(5L)).thenReturn(10L);

        mockMvc.perform(get("/projects/count/completed")
                        .param("userId", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }

    @Test
    void countCreatedProjects_returnsLong() throws Exception {
        when(projectService.countCreatedProjectsByClient(5L)).thenReturn(15L);

        mockMvc.perform(get("/projects/count/created")
                        .param("userId", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("15"));
    }
}
