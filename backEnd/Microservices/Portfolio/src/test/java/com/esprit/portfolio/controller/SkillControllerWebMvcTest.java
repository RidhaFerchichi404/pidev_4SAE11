package com.esprit.portfolio.controller;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.esprit.portfolio.entity.Domain;
import com.esprit.portfolio.entity.Skill;
import com.esprit.portfolio.service.SkillService;

@WebMvcTest(
        value = SkillController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class SkillControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkillService skillService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void domainsReturnsList() throws Exception {
        when(skillService.getAllDomains()).thenReturn(List.of(Domain.values()));
        mockMvc.perform(get("/api/skills/domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllSkillsReturnsOk() throws Exception {
        when(skillService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getSkillByIdReturnsSkill() throws Exception {
        Skill s = new Skill();
        s.setId(1L);
        s.setName("Java");
        s.setUserId(10L);
        when(skillService.findById(1L)).thenReturn(s);
        mockMvc.perform(get("/api/skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Java"));
    }

    @Test
    void getSkillsByUser() throws Exception {
        when(skillService.findAllByUserId(5L)).thenReturn(List.of());
        mockMvc.perform(get("/api/skills/user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void statsByDomainAndUsage() throws Exception {
        when(skillService.getSkillStatsByDomain()).thenReturn(List.of());
        when(skillService.getSkillUsageStats()).thenReturn(List.of());
        mockMvc.perform(get("/api/skills/stats/by-domain")).andExpect(status().isOk());
        mockMvc.perform(get("/api/skills/stats/usage")).andExpect(status().isOk());
    }

    @Test
    void statsSuccess() throws Exception {
        when(skillService.getSkillSuccessStats()).thenReturn(List.of());
        mockMvc.perform(get("/api/skills/stats/success")).andExpect(status().isOk());
    }

    @Test
    void patchDomains() throws Exception {
        Skill updated = new Skill();
        updated.setId(2L);
        when(skillService.updateSkillDomains(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(updated);
        mockMvc.perform(patch("/api/skills/2/domains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(Domain.WEB_DEVELOPMENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void batchByIds() throws Exception {
        when(skillService.findAllByIds(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());
        mockMvc.perform(post("/api/skills/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk());
    }

    @Test
    void batchEmptyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/skills/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSkill() throws Exception {
        Skill body = new Skill();
        body.setName("Python");
        body.setUserId(1L);
        Skill saved = new Skill();
        saved.setId(99L);
        saved.setName("Python");
        when(skillService.create(any(Skill.class))).thenReturn(saved);
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99));
    }

    @Test
    void updateSkill() throws Exception {
        Skill body = new Skill();
        body.setName("Java");
        Skill out = new Skill();
        out.setId(2L);
        when(skillService.update(eq(2L), any(Skill.class))).thenReturn(out);
        mockMvc.perform(put("/api/skills/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void deleteSkill() throws Exception {
        mockMvc.perform(delete("/api/skills/3")).andExpect(status().isNoContent());
    }
}
