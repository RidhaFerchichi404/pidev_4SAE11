package com.esprit.portfolio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

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
import com.esprit.portfolio.dto.ExperienceRequest;
import com.esprit.portfolio.entity.Experience;
import com.esprit.portfolio.service.ExperienceService;

@WebMvcTest(
        value = ExperienceController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class ExperienceControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExperienceService experienceService;

    @Test
    void statsByDomain() throws Exception {
        when(experienceService.getExperienceStatsByDomain()).thenReturn(List.of());
        mockMvc.perform(get("/api/experiences/stats/by-domain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAll() throws Exception {
        when(experienceService.getAllExperiences()).thenReturn(List.of());
        mockMvc.perform(get("/api/experiences"))
                .andExpect(status().isOk());
    }

    @Test
    void getByUser() throws Exception {
        when(experienceService.getExperiencesByUserId(3L)).thenReturn(List.of());
        mockMvc.perform(get("/api/experiences/user/3"))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdFound() throws Exception {
        Experience e = new Experience();
        e.setId(10L);
        when(experienceService.getExperienceById(10L)).thenReturn(Optional.of(e));
        mockMvc.perform(get("/api/experiences/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getByIdNotFound() throws Exception {
        when(experienceService.getExperienceById(11L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/experiences/11"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createExperience() throws Exception {
        Experience created = new Experience();
        created.setId(1L);
        ExperienceRequest req = new ExperienceRequest();
        when(experienceService.createExperience(any(ExperienceRequest.class))).thenReturn(created);
        mockMvc.perform(post("/api/experiences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteExperience() throws Exception {
        mockMvc.perform(delete("/api/experiences/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateExperience() throws Exception {
        Experience updated = new Experience();
        updated.setId(6L);
        ExperienceRequest req = new ExperienceRequest();
        when(experienceService.updateExperience(eq(6L), any(ExperienceRequest.class))).thenReturn(updated);
        mockMvc.perform(put("/api/experiences/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6));
    }
}
