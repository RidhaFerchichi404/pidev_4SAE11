package org.example.subcontracting.service;

import org.example.subcontracting.client.NotificationFeignClient;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.coach.*;
import org.example.subcontracting.coach.dto.DebitResultResponse;
import org.example.subcontracting.coach.dto.RechargeRequestDto;
import org.example.subcontracting.coach.entity.CoachFeatureCost;
import org.example.subcontracting.coach.entity.CoachWallet;
import org.example.subcontracting.coach.entity.CoachWalletTransaction;
import org.example.subcontracting.coach.repository.CoachFeatureCostRepository;
import org.example.subcontracting.coach.repository.CoachWalletRepository;
import org.example.subcontracting.coach.repository.CoachWalletTransactionRepository;
import org.example.subcontracting.entity.*;
import org.example.subcontracting.repository.SubcontractDeliverableRepository;
import org.example.subcontracting.repository.SubcontractMessageRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubcontractCoreServicesCoverageTest {
    private static final String FEATURE = "RISK_DEEP_ANALYSIS";

    @Mock private CoachWalletRepository walletRepository;
    @Mock private CoachWalletTransactionRepository txRepository;
    @Mock private CoachFeatureCostRepository featureCostRepository;
    @Mock private CoachNotificationDispatcher notificationDispatcher;
    @InjectMocks private CoachWalletService coachWalletService;

    @Mock private SubcontractRepository subcontractRepository;
    @Mock private SubcontractMessageRepository messageRepository;
    @Mock private UserFeignClient userFeignClient;
    @Mock private NotificationFeignClient notificationFeignClient;
    @Mock private SubcontractEmailService subcontractEmailService;
    @InjectMocks private SubcontractChatService subcontractChatService;

    @Mock private SubcontractDeliverableRepository deliverableRepository;
    @InjectMocks private SubcontractDashboardService dashboardService;

    @Test
    void coachWalletServiceCoversCreateDebitCreditAdminScanPaths() {
        CoachWalletProperties props = new CoachWalletProperties();
        props.setWelcomeBonusPoints(100);
        props.setLowBalanceThreshold(20);
        props.setCriticalBalanceThreshold(0);

        coachWalletService = new CoachWalletService(
                walletRepository, txRepository, featureCostRepository, props, notificationDispatcher
        );

        CoachWallet wallet = CoachWallet.builder().id(1L).userId(7L).balance(50).blocked(false).firstFreeUsed(false).lowBalanceAlerted(false).build();
        when(walletRepository.findByUserId(7L)).thenReturn(Optional.empty(), Optional.of(wallet), Optional.of(wallet), Optional.of(wallet), Optional.of(wallet));
        when(walletRepository.save(any(CoachWallet.class))).thenAnswer(inv -> {
            CoachWallet w = inv.getArgument(0);
            if (w.getId() == null) w.setId(1L);
            return w;
        });
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(featureCostRepository.findAllByOrderByFeatureCodeAsc()).thenReturn(List.of(
                CoachFeatureCost.builder().featureCode(FEATURE).active(true).costPoints(10).build()
        ));
        when(featureCostRepository.findByFeatureCodeAndActiveIsTrue(FEATURE))
                .thenReturn(Optional.of(CoachFeatureCost.builder().featureCode(FEATURE).active(true).costPoints(10).build()));
        when(txRepository.findByPerformedByRoleIgnoreCaseOrderByCreatedAtDesc(eq("ADMIN"), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(walletRepository.findAllByOrderByUpdatedAtDesc(any())).thenReturn(new PageImpl<>(List.of(wallet)));
        when(walletRepository.findLowBalanceNotAlerted(20)).thenReturn(List.of(wallet));

        var built = coachWalletService.buildWalletResponse(7L);
        assertThat(built.getBalance()).isNotNull();

        DebitResultResponse debit = coachWalletService.debitForFeature(7L, CoachFeatureCode.RISK_DEEP_ANALYSIS, "{\"x\":1}");
        assertThat(debit.isSuccess()).isTrue();

        coachWalletService.creditAdmin(7L, 20, "TOPUP", 99L, "note");
        coachWalletService.debitAdmin(7L, 5, "ADJ", 99L);
        coachWalletService.setBlocked(7L, true, 99L);
        coachWalletService.setBlocked(7L, false, 99L);
        coachWalletService.saveInitialFreeConsumption(7L, "meta");
        RechargeRequestDto rr = new RechargeRequestDto();
        rr.setSuggestedPoints(200);
        rr.setMessage("need points");
        coachWalletService.requestRecharge(7L, rr);
        int alerted = coachWalletService.scanLowBalanceWallets();

        assertThat(alerted).isEqualTo(1);
        assertThat(coachWalletService.listFeatureCosts()).hasSize(1);
        assertThat(coachWalletService.remainingAnalyses(7L)).containsKey(FEATURE);
        assertThat(coachWalletService.adminListWallets(PageRequest.of(0, 10)).getContent()).isNotEmpty();
        assertThat(coachWalletService.adminAudit(PageRequest.of(0, 10))).isNotNull();
    }

    @Test
    void subcontractChatServiceCoversMessagesSendAndValidation() {
        Subcontract sc = new Subcontract();
        sc.setId(10L);
        sc.setMainFreelancerId(1L);
        sc.setSubcontractorId(2L);
        sc.setTitle("Chat flow");
        when(subcontractRepository.findById(10L)).thenReturn(Optional.of(sc));

        SubcontractMessage msg = SubcontractMessage.builder().id(3L).subcontract(sc).senderUserId(1L).senderName("Main User").message("hello").build();
        when(messageRepository.findBySubcontractIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(msg));
        when(messageRepository.save(any(SubcontractMessage.class))).thenAnswer(inv -> {
            SubcontractMessage m = inv.getArgument(0);
            m.setId(99L);
            return m;
        });
        when(userFeignClient.getUserById(1L)).thenReturn(user(1L, "Main", "User", "main@x.tn"));
        when(userFeignClient.getUserById(2L)).thenReturn(user(2L, "Sub", "User", "sub@x.tn"));

        assertThat(subcontractChatService.getMessages(10L, 1L)).hasSize(1);
        assertThat(subcontractChatService.sendMessage(10L, 1L, "bonjour").getId()).isEqualTo(99L);
        verify(notificationFeignClient).sendNotification(any());
        verify(subcontractEmailService).sendChatMessageEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        assertThatThrownBy(() -> subcontractChatService.sendMessage(10L, 1L, " "))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> subcontractChatService.getMessages(10L, 77L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void subcontractDashboardServiceCoversScoreAndDashboard() {
        Subcontract s = new Subcontract();
        s.setId(10L);
        s.setSubcontractorId(2L);
        s.setMainFreelancerId(1L);
        s.setCategory(SubcontractCategory.DEVELOPMENT);
        s.setStatus(SubcontractStatus.COMPLETED);
        when(subcontractRepository.findBySubcontractorIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(s));
        when(subcontractRepository.findAll()).thenReturn(List.of(s));
        when(deliverableRepository.findBySubcontractIdOrderByDeadlineAsc(10L)).thenReturn(List.of(
                deliverable(DeliverableStatus.APPROVED), deliverable(DeliverableStatus.REJECTED)
        ));
        when(userFeignClient.getUserById(2L)).thenReturn(user(2L, "Sub", "One", "x@x.tn"));

        var score = dashboardService.computeScore(2L);
        assertThat(score.getScore()).isBetween(0, 100);
        assertThat(score.getBreakdown()).isNotEmpty();

        var dash = dashboardService.buildDashboard();
        assertThat(dash.getTotalSubcontracts()).isEqualTo(1);
        assertThat(dash.getByStatus()).containsKey(SubcontractStatus.COMPLETED.name());
    }

    private static UserRemoteDto user(Long id, String first, String last, String email) {
        UserRemoteDto u = new UserRemoteDto();
        u.setId(id);
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        return u;
    }

    private static SubcontractDeliverable deliverable(DeliverableStatus status) {
        SubcontractDeliverable d = new SubcontractDeliverable();
        d.setStatus(status);
        d.setDeadline(java.time.LocalDate.now().minusDays(1));
        return d;
    }
}
