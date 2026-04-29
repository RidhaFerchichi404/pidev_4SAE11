package org.example.offer.controller;

import org.example.offer.dto.request.ChatAssistantRequest;
import org.example.offer.dto.response.ChatAssistantResponse;
import org.example.offer.dto.response.DashboardStatsResponse;
import org.example.offer.service.ChatAssistantService;
import org.example.offer.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControllerSmokeUnitTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private ChatAssistantService chatAssistantService;

    @InjectMocks
    private DashboardController dashboardController;

    @InjectMocks
    private ChatAssistantController chatAssistantController;

    @Test
    void dashboardController_returnsStatsPayload() {
        DashboardStatsResponse stats = new DashboardStatsResponse(2, 1, BigDecimal.TEN, 4, 1, 3);
        when(dashboardService.getFreelancerDashboardStats(9L)).thenReturn(stats);

        var response = dashboardController.getFreelancerStats(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(stats);
    }

    @Test
    void chatAssistantController_returnsNoContentWhenServiceReturnsNull() {
        ChatAssistantRequest request = new ChatAssistantRequest();
        request.setMessage("hello");
        when(chatAssistantService.getReply(request)).thenReturn(null);

        var response = chatAssistantController.chat(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void chatAssistantController_returnsReplyWhenPresent() {
        ChatAssistantRequest request = new ChatAssistantRequest();
        request.setMessage("hello");
        ChatAssistantResponse payload = new ChatAssistantResponse("Hi there");
        when(chatAssistantService.getReply(request)).thenReturn(payload);

        var response = chatAssistantController.chat(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(payload);
    }
}
