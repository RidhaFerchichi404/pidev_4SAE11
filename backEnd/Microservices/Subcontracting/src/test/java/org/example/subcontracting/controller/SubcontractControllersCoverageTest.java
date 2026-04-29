package org.example.subcontracting.controller;

import org.example.subcontracting.dto.request.DeliverableRequest;
import org.example.subcontracting.dto.request.SubcontractRequest;
import org.example.subcontracting.dto.response.DeliverableResponse;
import org.example.subcontracting.dto.response.SubcontractResponse;
import org.example.subcontracting.service.SubcontractAiDecisionAssistantService;
import org.example.subcontracting.service.SubcontractAiMatchService;
import org.example.subcontracting.service.SubcontractAuditService;
import org.example.subcontracting.service.SubcontractCoachingService;
import org.example.subcontracting.service.SubcontractDashboardService;
import org.example.subcontracting.service.SubcontractFinancialAnalysisService;
import org.example.subcontracting.service.SubcontractNotificationService;
import org.example.subcontracting.service.SubcontractPredictiveDashboardService;
import org.example.subcontracting.service.SubcontractRiskCockpitService;
import org.example.subcontracting.service.SubcontractService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubcontractControllersCoverageTest {
    @Mock private SubcontractService subcontractService;
    @Mock private SubcontractDashboardService dashboardService;
    @Mock private SubcontractNotificationService notificationService;
    @Mock private SubcontractAuditService auditService;
    @Mock private SubcontractAiMatchService aiMatchService;
    @Mock private SubcontractFinancialAnalysisService financialAnalysisService;
    @Mock private SubcontractRiskCockpitService riskCockpitService;
    @Mock private SubcontractPredictiveDashboardService predictiveDashboardService;
    @Mock private SubcontractCoachingService coachingService;
    @Mock private SubcontractAiDecisionAssistantService aiDecisionAssistantService;
    @InjectMocks private SubcontractController subcontractController;
    @InjectMocks private DeliverableController deliverableController;

    @Test
    void subcontractControllerCrudAndWorkflowEndpoints() {
        SubcontractResponse response = new SubcontractResponse();
        when(subcontractService.create(anyLong(), any(SubcontractRequest.class))).thenReturn(response);
        when(subcontractService.getById(anyLong())).thenReturn(response);
        when(subcontractService.getAll()).thenReturn(List.of(response));
        when(subcontractService.getByMainFreelancer(anyLong())).thenReturn(List.of(response));
        when(subcontractService.getBySubcontractor(anyLong())).thenReturn(List.of(response));
        when(subcontractService.getByProject(anyLong())).thenReturn(List.of(response));
        when(subcontractService.getByStatus(any())).thenReturn(List.of(response));
        when(subcontractService.update(anyLong(), any(SubcontractRequest.class))).thenReturn(response);
        when(subcontractService.propose(anyLong())).thenReturn(response);
        when(subcontractService.accept(anyLong())).thenReturn(response);
        when(subcontractService.reject(anyLong(), any())).thenReturn(response);
        when(subcontractService.startWork(anyLong())).thenReturn(response);
        when(subcontractService.complete(anyLong())).thenReturn(response);
        when(subcontractService.cancel(anyLong(), any())).thenReturn(response);
        when(subcontractService.close(anyLong())).thenReturn(response);
        when(subcontractService.reopen(anyLong())).thenReturn(response);

        assertThat(subcontractController.create(1L, new SubcontractRequest()).getStatusCodeValue()).isEqualTo(201);
        assertThat(subcontractController.getById(1L).getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.getAll().getBody()).hasSize(1);
        assertThat(subcontractController.getByMainFreelancer(1L).getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.getBySubcontractor(1L).getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.getByProject(1L).getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.getByStatus("OPEN").getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.update(1L, new SubcontractRequest()).getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.delete(1L).getStatusCodeValue()).isEqualTo(204);
        assertThat(subcontractController.propose(1L).getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.accept(1L).getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.reject(1L, "x").getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.startWork(1L).getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.complete(1L).getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.cancel(1L, "x").getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.close(1L).getStatusCodeValue()).isEqualTo(200);
        assertThat(subcontractController.reopen(1L).getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    void deliverableControllerEndpointsDelegate() {
        DeliverableResponse response = new DeliverableResponse();
        when(subcontractService.addDeliverable(anyLong(), any(DeliverableRequest.class))).thenReturn(response);
        when(subcontractService.getDeliverables(anyLong())).thenReturn(List.of(response));
        when(subcontractService.updateDeliverable(anyLong(), any(DeliverableRequest.class))).thenReturn(response);
        when(subcontractService.submitDeliverable(anyLong(), any())).thenReturn(response);
        when(subcontractService.reviewDeliverable(anyLong(), any())).thenReturn(response);

        assertThat(deliverableController.addDeliverable(1L, new DeliverableRequest()).getStatusCodeValue()).isEqualTo(201);
        assertThat(deliverableController.getDeliverables(1L).getBody()).hasSize(1);
        assertThat(deliverableController.updateDeliverable(1L, 2L, new DeliverableRequest()).getStatusCodeValue()).isEqualTo(200);
        assertThat(deliverableController.deleteDeliverable(1L, 2L).getStatusCodeValue()).isEqualTo(204);
        assertThat(deliverableController.submitDeliverable(1L, 2L, null).getStatusCodeValue()).isEqualTo(200);
        assertThat(deliverableController.reviewDeliverable(1L, 2L, null).getStatusCodeValue()).isEqualTo(200);
    }
}
