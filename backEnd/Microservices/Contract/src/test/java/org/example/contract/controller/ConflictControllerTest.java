package org.example.contract.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.contract.dto.ConflictStatsDto;
import org.example.contract.entity.Conflict;
import org.example.contract.service.ConflictService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ConflictController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
class ConflictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConflictService conflictService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllReturnsList() throws Exception {
        when(conflictService.getAllConflicts()).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/conflicts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getByIdReturnsConflict() throws Exception {
        Conflict c = new Conflict();
        c.setId(1L);
        when(conflictService.getConflictById(1L)).thenReturn(c);
        mockMvc.perform(get("/api/conflicts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getByIdReturns404WhenServiceThrows() throws Exception {
        when(conflictService.getConflictById(2L)).thenThrow(new RuntimeException("nf"));
        mockMvc.perform(get("/api/conflicts/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createConflictReturnsConflict() throws Exception {
        Conflict in = new Conflict();
        Conflict out = new Conflict();
        out.setId(3L);
        when(conflictService.createConflict(eq(10L), any(Conflict.class))).thenReturn(out);
        mockMvc.perform(post("/api/conflicts/contract/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void getConflictsByContractId() throws Exception {
        when(conflictService.getConflictsByContractId(5L)).thenReturn(List.of());
        mockMvc.perform(get("/api/conflicts/contract/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getStats() throws Exception {
        ConflictStatsDto dto = new ConflictStatsDto();
        dto.setOpen(2);
        when(conflictService.getConflictStats()).thenReturn(dto);
        mockMvc.perform(get("/api/conflicts/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(2));
    }

    @Test
    void updateStatusCallsService() throws Exception {
        Conflict c = new Conflict();
        c.setId(4L);
        when(conflictService.updateConflictStatus(eq(4L), any())).thenReturn(c);
        mockMvc.perform(put("/api/conflicts/4/status")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }
}
