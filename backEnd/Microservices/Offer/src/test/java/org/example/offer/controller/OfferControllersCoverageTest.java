package org.example.offer.controller;

import org.example.offer.dto.request.OfferFilterRequest;
import org.example.offer.dto.request.OfferQuestionRequest;
import org.example.offer.dto.request.OfferRequest;
import org.example.offer.dto.request.TranslateTextsRequest;
import org.example.offer.dto.response.OfferApplicationResponse;
import org.example.offer.dto.response.OfferQuestionResponse;
import org.example.offer.dto.response.OfferResponse;
import org.example.offer.entity.ApplicationStatus;
import org.example.offer.entity.OfferStatus;
import org.example.offer.service.OfferApplicationService;
import org.example.offer.service.OfferQuestionService;
import org.example.offer.service.OfferService;
import org.example.offer.service.SmartMatchingService;
import org.example.offer.service.TranslationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferControllersCoverageTest {

    @Mock private OfferService offerService;
    @Mock private TranslationService translationService;
    @Mock private SmartMatchingService smartMatchingService;
    @Mock private OfferQuestionService offerQuestionService;
    @Mock private OfferApplicationService applicationService;
    @InjectMocks private OfferController offerController;
    @InjectMocks private OfferApplicationController offerApplicationController;

    @Test
    void offerController_endpointsReturnExpectedStatusCodes() {
        ReflectionTestUtils.setField(offerController, "welcomeMessage", "hello");
        OfferResponse offerResponse = new OfferResponse();
        when(offerService.createOffer(any(OfferRequest.class))).thenReturn(offerResponse);
        when(offerService.getOfferById(1L)).thenReturn(offerResponse);
        when(offerService.getActiveOffers(anyInt(), anyInt())).thenReturn(Page.empty());
        when(offerService.getOffersByFreelancer(anyLong())).thenReturn(List.of(offerResponse));
        when(offerService.getFeaturedOffers()).thenReturn(List.of(offerResponse));
        when(offerService.getTopRatedOffers(any(BigDecimal.class), anyInt(), anyInt())).thenReturn(Page.empty());
        when(offerService.searchOffers(any(OfferFilterRequest.class))).thenReturn(Page.empty());
        when(offerService.updateOffer(anyLong(), any(OfferRequest.class))).thenReturn(offerResponse);
        when(offerService.publishOffer(anyLong(), anyLong())).thenReturn(offerResponse);
        when(offerService.changeOfferStatus(anyLong(), any(OfferStatus.class), anyLong())).thenReturn(offerResponse);
        when(offerService.updateScores(anyLong(), any(), any())).thenReturn(offerResponse);
        when(offerService.translateOffer(anyLong(), any())).thenReturn(null);
        when(translationService.translate(any(), any())).thenReturn(List.of("x"));
        when(smartMatchingService.getRecommendedOffersForClient(anyLong(), anyInt())).thenReturn(List.of(offerResponse));
        when(offerQuestionService.getQuestionsByOfferId(anyLong())).thenReturn(List.of(new OfferQuestionResponse()));
        when(offerQuestionService.addQuestion(anyLong(), anyLong(), any(OfferQuestionRequest.class))).thenReturn(new OfferQuestionResponse());

        assertThat(offerController.welcome()).isEqualTo("hello");
        assertThat(offerController.createOffer(new OfferRequest()).getStatusCode().value()).isEqualTo(201);
        assertThat(offerController.getOfferById(1L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.getActiveOffers(0, 10).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.getOffersByFreelancer(1L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.getFeaturedOffers().getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.getTopRatedOffers(BigDecimal.ONE, 0, 10).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.searchOffers(new OfferFilterRequest()).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.updateOffer(1L, new OfferRequest()).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.publishOffer(1L, 2L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.changeOfferStatus(1L, OfferStatus.ACCEPTED, 2L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.updateScores(1L, BigDecimal.ONE, BigDecimal.ONE).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.translateOffer(1L, Map.of("targetLanguage", "en")).getStatusCode().value()).isEqualTo(200);
        TranslateTextsRequest req = new TranslateTextsRequest();
        req.setTexts(List.of("a"));
        req.setTargetLanguage("en");
        assertThat(offerController.translateTexts(req).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.getRecommendedOffers(1L, 2).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.recordOfferView(Map.of("clientId", 1L, "offerId", 2L)).getStatusCode().value()).isEqualTo(204);
        assertThat(offerController.getOfferQuestions(1L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerController.addOfferQuestion(1L, 2L, new OfferQuestionRequest()).getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void offerApplicationController_smokeCoverage() {
        OfferApplicationResponse app = new OfferApplicationResponse();
        when(applicationService.applyToOffer(any())).thenReturn(app);
        when(applicationService.listAcceptedApplicationsForFreelancerOwnedOffers(anyLong())).thenReturn(List.of(app));
        when(applicationService.getApplicationById(anyLong())).thenReturn(app);
        when(applicationService.getApplicationsByOffer(anyLong(), anyInt(), anyInt())).thenReturn(Page.empty());
        when(applicationService.getApplicationsByClient(anyLong(), anyInt(), anyInt())).thenReturn(Page.empty());
        when(applicationService.getPendingApplications()).thenReturn(List.of(app));
        when(applicationService.getUnreadApplicationsByFreelancer(anyLong())).thenReturn(List.of(app));
        when(applicationService.getApplicationsByOfferAndStatus(anyLong(), any(ApplicationStatus.class))).thenReturn(List.of(app));
        when(applicationService.countPendingApplications(anyLong())).thenReturn(1L);
        when(applicationService.getRecentApplications()).thenReturn(List.of(app));
        when(applicationService.acceptApplication(anyLong(), anyLong())).thenReturn(app);
        when(applicationService.rejectApplication(anyLong(), anyLong(), any())).thenReturn(app);
        when(applicationService.shortlistApplication(anyLong(), anyLong())).thenReturn(app);
        when(applicationService.markAsRead(anyLong(), anyLong())).thenReturn(app);
        when(applicationService.withdrawApplication(anyLong(), anyLong())).thenReturn(app);
        when(applicationService.updateApplication(anyLong(), any())).thenReturn(app);

        assertThat(offerApplicationController.applyToOffer(null).getStatusCode().value()).isEqualTo(201);
        assertThat(offerApplicationController.listAcceptedForFreelancerOwnedOffers(1L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.getApplicationById(1L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.getApplicationsByOffer(1L, 0, 10).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.getApplicationsByClient(1L, 0, 10).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.getPendingApplications().getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.getUnreadApplicationsByFreelancer(1L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.getApplicationsByOfferAndStatus(1L, ApplicationStatus.PENDING).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.countPendingApplications(1L).getBody()).isEqualTo(1L);
        assertThat(offerApplicationController.getRecentApplications().getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.acceptApplication(1L, 1L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.rejectApplication(1L, 1L, "x").getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.shortlistApplication(1L, 1L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.markAsRead(1L, 1L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.withdrawApplication(1L, 1L).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.updateApplication(1L, null).getStatusCode().value()).isEqualTo(200);
        assertThat(offerApplicationController.deleteApplication(1L, 1L).getStatusCode().value()).isEqualTo(204);
    }
}
