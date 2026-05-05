package com.esprit.aimodel.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.esprit.aimodel.dto.AiLiveStatus;
import com.esprit.aimodel.service.AiService;
import com.esprit.aimodel.service.ProviderStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = AiController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
class AiControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiService aiService;

    @MockitoBean
    private ProviderStatusService providerStatusService;

    @Test
    void statusReturnsLiveShape() throws Exception {
        when(providerStatusService.liveStatus())
                .thenReturn(new AiLiveStatus("aimodel", "UP", true, "gemma3:4b", true));
        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("aimodel"))
                .andExpect(jsonPath("$.ollamaReachable").value(true))
                .andExpect(jsonPath("$.modelReady").value(true));
    }

    @Test
    void generateReturnsSuccessEnvelope() throws Exception {
        when(aiService.generateResponse(anyString(), any())).thenReturn("hello");
        mockMvc.perform(post("/api/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("hello"));
    }

    @Test
    void generateInvalidJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/ai/generate").contentType(MediaType.APPLICATION_JSON).content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("Invalid JSON body"));
    }
}
