package com.esprit.portfolio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.esprit.portfolio.dto.TestSubmission;
import com.esprit.portfolio.entity.Evaluation;
import com.esprit.portfolio.service.EvaluationService;

@WebMvcTest(
        value = EvaluationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class EvaluationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EvaluationService evaluationService;

    @Test
    void getAll() throws Exception {
        when(evaluationService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/evaluations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getById() throws Exception {
        Evaluation e = new Evaluation();
        e.setId(3L);
        when(evaluationService.findById(3L)).thenReturn(e);
        mockMvc.perform(get("/api/evaluations/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void getEvaluationsByFreelancer() throws Exception {
        when(evaluationService.findByFreelancerId(44L)).thenReturn(List.of());
        mockMvc.perform(get("/api/evaluations/freelancer/44"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getByFreelancerAndSkillFound() throws Exception {
        Evaluation e = new Evaluation();
        e.setId(1L);
        when(evaluationService.findByFreelancerIdAndSkillId(2L, 5L)).thenReturn(e);
        mockMvc.perform(get("/api/evaluations/freelancer/2/skill/5"))
                .andExpect(status().isOk());
    }

    @Test
    void getByFreelancerAndSkillNotFound() throws Exception {
        when(evaluationService.findByFreelancerIdAndSkillId(2L, 5L)).thenReturn(null);
        mockMvc.perform(get("/api/evaluations/freelancer/2/skill/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEvaluation() throws Exception {
        Evaluation body = new Evaluation();
        Evaluation saved = new Evaluation();
        saved.setId(9L);
        when(evaluationService.create(any(Evaluation.class), eq(7L))).thenReturn(saved);
        mockMvc.perform(post("/api/evaluations/skill/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void submitEvaluation() throws Exception {
        TestSubmission sub = new TestSubmission();
        Evaluation out = new Evaluation();
        out.setId(4L);
        when(evaluationService.submitEvaluation(any(TestSubmission.class))).thenReturn(out);
        mockMvc.perform(post("/api/evaluations/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void deleteEvaluation() throws Exception {
        mockMvc.perform(delete("/api/evaluations/8"))
                .andExpect(status().isNoContent());
    }
}
