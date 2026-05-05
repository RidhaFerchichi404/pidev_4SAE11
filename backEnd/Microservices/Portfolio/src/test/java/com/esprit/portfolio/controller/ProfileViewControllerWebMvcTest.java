package com.esprit.portfolio.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.esprit.portfolio.dto.ProfileViewRequest;
import com.esprit.portfolio.dto.ProfileViewStats;
import com.esprit.portfolio.service.ProfileViewService;

@WebMvcTest(
        value = ProfileViewController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class ProfileViewControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileViewService profileViewService;

    @Test
    void recordView() throws Exception {
        ProfileViewRequest req = new ProfileViewRequest();
        req.setProfileUserId(1L);
        req.setViewerId(2L);
        mockMvc.perform(post("/api/views/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(profileViewService).recordView(1L, 2L);
    }

    @Test
    void getTotalCount() throws Exception {
        when(profileViewService.getTotalViewCount(5L)).thenReturn(11L);
        mockMvc.perform(get("/api/views/user/5/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(11));
    }

    @Test
    void getRecentViewers() throws Exception {
        when(profileViewService.getRecentViewers(eq(3L), anyInt())).thenReturn(List.of());
        mockMvc.perform(get("/api/views/user/3/recent").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getStats() throws Exception {
        when(profileViewService.getTotalViewCount(2L)).thenReturn(100L);
        when(profileViewService.getThisWeekViewCount(2L)).thenReturn(5L);
        when(profileViewService.getLastWeekViewCount(2L)).thenReturn(3L);
        mockMvc.perform(get("/api/views/user/2/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalViews").value(100))
                .andExpect(jsonPath("$.thisWeekViews").value(5))
                .andExpect(jsonPath("$.lastWeekViews").value(3));
    }

    @Test
    void getDailyStats() throws Exception {
        when(profileViewService.getDailyStats(eq(4L), eq(14))).thenReturn(List.of());
        mockMvc.perform(get("/api/views/user/4/daily").param("days", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
