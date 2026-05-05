package com.esprit.portfolio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.esprit.portfolio.entity.EvaluationTest;
import com.esprit.portfolio.service.EvaluationTestService;

@WebMvcTest(
        value = EvaluationTestController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class EvaluationTestControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EvaluationTestService evaluationTestService;

    @Test
    void getAll() throws Exception {
        when(evaluationTestService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/evaluation-tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getById() throws Exception {
        EvaluationTest t = new EvaluationTest();
        t.setId(1L);
        t.setTitle("T");
        when(evaluationTestService.findById(1L)).thenReturn(t);
        mockMvc.perform(get("/api/evaluation-tests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getBySkill() throws Exception {
        when(evaluationTestService.findBySkillId(5L)).thenReturn(List.of());
        mockMvc.perform(get("/api/evaluation-tests/skill/5"))
                .andExpect(status().isOk());
    }

    @Test
    void createForSkill() throws Exception {
        EvaluationTest body = new EvaluationTest();
        EvaluationTest saved = new EvaluationTest();
        saved.setId(9L);
        when(evaluationTestService.create(any(EvaluationTest.class), eq(3L))).thenReturn(saved);
        mockMvc.perform(post("/api/evaluation-tests/skill/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void generateForSkill() throws Exception {
        EvaluationTest gen = new EvaluationTest();
        gen.setId(10L);
        when(evaluationTestService.generateTestForSkill(4L)).thenReturn(gen);
        mockMvc.perform(post("/api/evaluation-tests/generate/4"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void updateTest() throws Exception {
        EvaluationTest body = new EvaluationTest();
        EvaluationTest out = new EvaluationTest();
        out.setId(2L);
        when(evaluationTestService.update(eq(2L), any(EvaluationTest.class))).thenReturn(out);
        mockMvc.perform(put("/api/evaluation-tests/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void deleteTest() throws Exception {
        doNothing().when(evaluationTestService).delete(7L);
        mockMvc.perform(delete("/api/evaluation-tests/7"))
                .andExpect(status().isNoContent());
    }
}
