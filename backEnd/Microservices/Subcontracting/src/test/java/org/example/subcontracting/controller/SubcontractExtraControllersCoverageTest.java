package org.example.subcontracting.controller;

import org.example.subcontracting.coach.CoachInsightService;
import org.example.subcontracting.coach.CoachWalletService;
import org.example.subcontracting.coach.controller.CoachFeatureCostController;
import org.example.subcontracting.coach.controller.CoachInsightController;
import org.example.subcontracting.coach.controller.CoachWalletController;
import org.example.subcontracting.coach.dto.*;
import org.example.subcontracting.coach.entity.CoachFeatureCost;
import org.example.subcontracting.coach.entity.CoachInsightHistory;
import org.example.subcontracting.coach.entity.CoachWallet;
import org.example.subcontracting.dto.request.SubcontractMessageRequest;
import org.example.subcontracting.dto.response.MediaUploadResponse;
import org.example.subcontracting.dto.response.SubcontractMessageResponse;
import org.example.subcontracting.service.SubcontractChatService;
import org.example.subcontracting.service.SubcontractMediaStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubcontractExtraControllersCoverageTest {
    private static final String MEDIA_FILE = "a.mp4";
    private static final String MEDIA_TYPE = "video/mp4";

    @Mock private SubcontractChatService chatService;
    @Mock private SubcontractMediaStorageService mediaStorageService;
    @Mock private CoachWalletService coachWalletService;
    @Mock private CoachInsightService coachInsightService;

    @InjectMocks private SubcontractChatController chatController;
    @InjectMocks private SubcontractMediaController mediaController;
    @InjectMocks private CoachWalletController walletController;
    @InjectMocks private CoachFeatureCostController featureCostController;
    @InjectMocks private CoachInsightController insightController;

    @Test
    void subcontractChatAndMediaControllersCoverHappyAndValidationPaths() {
        SubcontractMessageResponse msg = SubcontractMessageResponse.builder().id(10L).message("ok").build();
        when(chatService.getMessages(1L, 2L)).thenReturn(List.of(msg));
        when(chatService.sendMessage(1L, 2L, "hello")).thenReturn(msg);

        var listRes = chatController.getMessages(1L, 2L);
        assertThat(listRes.getBody()).hasSize(1);
        SubcontractMessageRequest req = new SubcontractMessageRequest();
        req.setMessage("hello");
        assertThat(chatController.sendMessage(1L, 2L, req).getStatusCode().value()).isEqualTo(201);

        ReflectionTestUtils.setField(mediaController, "downloadBaseUrl", "http://localhost:8110/");
        when(mediaStorageService.save(any())).thenReturn(new SubcontractMediaStorageService.StoredMedia(MEDIA_FILE, org.example.subcontracting.entity.SubcontractMediaType.VIDEO, MEDIA_TYPE));
        when(mediaStorageService.loadAsResource(MEDIA_FILE)).thenReturn(new ByteArrayResource(new byte[]{1}));
        when(mediaStorageService.guessContentType(MEDIA_FILE)).thenReturn(MEDIA_TYPE);

        var badUpload = mediaController.upload(0L, new MockMultipartFile("f", "x.mp4", MEDIA_TYPE, new byte[]{1}));
        assertThat(badUpload.getStatusCode().value()).isEqualTo(400);
        var okUpload = mediaController.upload(9L, new MockMultipartFile("f", "x.mp4", MEDIA_TYPE, new byte[]{1}));
        assertThat(okUpload.getBody()).isNotNull();
        assertThat(okUpload.getBody().getMediaUrl()).contains("/api/subcontracts/media/files/" + MEDIA_FILE);
        assertThat(mediaController.stream(MEDIA_FILE).getStatusCode().value()).isEqualTo(200);
        when(mediaStorageService.loadAsResource("missing")).thenReturn(null);
        assertThat(mediaController.stream("missing").getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void coachWalletAndFeatureCostControllersCoverAdminAndValidation() {
        when(coachWalletService.buildWalletResponse(1L)).thenReturn(WalletResponse.builder().build());
        when(coachWalletService.myTransactions(eq(1L), any())).thenReturn(new PageImpl<>(List.of()));
        when(coachWalletService.remainingAnalyses(1L)).thenReturn(Map.of());
        when(coachWalletService.adminListWallets(any())).thenReturn(new PageImpl<>(List.of(new CoachWallet())));
        when(coachWalletService.adminGetWallet(1L)).thenReturn(new CoachWallet());
        when(coachWalletService.adminUserTransactions(eq(1L), any())).thenReturn(new PageImpl<>(List.of()));
        when(coachWalletService.adminAudit(any())).thenReturn(new PageImpl<>(List.of()));
        when(coachWalletService.listFeatureCosts()).thenReturn(List.of(CoachFeatureCost.builder().featureCode("X").costPoints(10).build()));
        when(coachWalletService.updateFeatureCost(eq("X"), eq(5), eq(1L))).thenReturn(CoachFeatureCost.builder().featureCode("X").costPoints(5).build());

        assertThat(walletController.me(1L).getStatusCode().value()).isEqualTo(200);
        assertThat(walletController.myTransactions(1L, 0, 10).getStatusCode().value()).isEqualTo(200);
        assertThat(walletController.remaining(1L).getStatusCode().value()).isEqualTo(200);
        assertThat(walletController.rechargeRequest(1L, new RechargeRequestDto()).getStatusCode().value()).isEqualTo(202);
        assertThat(walletController.adminAll(1L, 0, 10).getStatusCode().value()).isEqualTo(200);
        assertThat(walletController.adminWallet(1L, 1L).getStatusCode().value()).isEqualTo(200);
        assertThat(walletController.adminUserTx(1L, 1L, 0, 10).getStatusCode().value()).isEqualTo(200);
        assertThat(walletController.adminAudit(1L, 0, 10).getStatusCode().value()).isEqualTo(200);
        assertThat(walletController.adminBlock(1L, 1L, true).getStatusCode().value()).isEqualTo(200);
        assertThatThrownBy(() -> walletController.adminAll(0L, 0, 10)).isInstanceOf(ResponseStatusException.class);

        AdminCreditRequest credit = new AdminCreditRequest();
        credit.setAmount(5);
        credit.setReason("x");
        AdminDebitRequest debit = new AdminDebitRequest();
        debit.setAmount(2);
        debit.setReason("x");
        assertThat(walletController.adminCredit(1L, 1L, credit).getStatusCode().value()).isEqualTo(200);
        assertThat(walletController.adminDebit(1L, 1L, debit).getStatusCode().value()).isEqualTo(200);

        FeatureCostPatchRequest patch = new FeatureCostPatchRequest();
        patch.setCostPoints(5);
        assertThat(featureCostController.list().getBody()).hasSize(1);
        assertThat(featureCostController.patch(1L, "X", patch).getStatusCode().value()).isEqualTo(200);
        assertThatThrownBy(() -> featureCostController.patch(0L, "X", patch)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void coachInsightControllerCoversEndpointsAndHistoryMapping() {
        when(coachInsightService.initialInsight(eq(1L), any())).thenReturn(new CoachInsightResponse());
        when(coachInsightService.advancedInsight(eq(1L), any())).thenReturn(new CoachInsightResponse());
        CoachInsightHistory h = CoachInsightHistory.builder()
                .id(9L).userId(1L).subcontractId(2L).featureCode("INITIAL")
                .free(true).pointsSpent(0).insightResultJson("{\"k\":1}")
                .createdAt(java.time.Instant.now()).build();
        when(coachInsightService.myHistory(eq(1L), any())).thenReturn(new PageImpl<>(List.of(h), PageRequest.of(0, 1), 1));
        when(coachInsightService.adminUserHistory(eq(1L), any())).thenReturn(new PageImpl<>(List.of(h), PageRequest.of(0, 1), 1));

        assertThat(insightController.initial(1L, CoachInsightRequest.builder().build()).getStatusCode().value()).isEqualTo(200);
        assertThat(insightController.advanced(1L, CoachInsightRequest.builder().build()).getStatusCode().value()).isEqualTo(200);
        assertThat(insightController.myHistory(1L, 0, 10).getBody().getTotalElements()).isEqualTo(1);
        assertThat(insightController.adminHistory(1L, 1L, 0, 10).getBody().getTotalElements()).isEqualTo(1);
        assertThatThrownBy(() -> insightController.adminHistory(0L, 1L, 0, 10)).isInstanceOf(ResponseStatusException.class);
    }
}
