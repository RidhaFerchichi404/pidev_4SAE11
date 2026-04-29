package org.example.offer.service;

import org.example.offer.dto.response.DashboardStatsResponse;
import org.example.offer.entity.ApplicationStatus;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferApplication;
import org.example.offer.entity.OfferStatus;
import org.example.offer.repository.OfferApplicationRepository;
import org.example.offer.repository.OfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private OfferRepository offerRepository;
    @Mock private OfferApplicationRepository applicationRepository;
    @Mock private OfferService offerService;
    @InjectMocks private DashboardService dashboardService;

    @Test
    void computesFreelancerDashboardStatsAndTrends() {
        Offer acceptedRecent = new Offer();
        acceptedRecent.setId(1L);
        acceptedRecent.setFreelancerId(8L);
        acceptedRecent.setOfferStatus(OfferStatus.ACCEPTED);
        acceptedRecent.setCreatedAt(LocalDateTime.now().minusDays(2));
        acceptedRecent.setPrice(BigDecimal.valueOf(150));

        Offer availableOld = new Offer();
        availableOld.setId(2L);
        availableOld.setFreelancerId(8L);
        availableOld.setOfferStatus(OfferStatus.AVAILABLE);
        availableOld.setCreatedAt(LocalDateTime.now().minusDays(40));
        availableOld.setPrice(BigDecimal.valueOf(80));

        OfferApplication pending = new OfferApplication();
        pending.setOffer(acceptedRecent);
        pending.setStatus(ApplicationStatus.PENDING);
        pending.setAppliedAt(LocalDateTime.now().minusDays(1));

        when(offerRepository.findByFreelancerIdAndOfferStatus(8L, OfferStatus.ACCEPTED)).thenReturn(List.of(acceptedRecent));
        when(offerRepository.findByFreelancerIdAndOfferStatus(8L, OfferStatus.AVAILABLE)).thenReturn(List.of(availableOld));
        when(offerRepository.findByFreelancerId(8L)).thenReturn(List.of(acceptedRecent, availableOld));
        when(applicationRepository.findByStatus(ApplicationStatus.PENDING)).thenReturn(List.of(pending));
        when(applicationRepository.findRecentApplications(any())).thenReturn(List.of(pending));
        when(offerRepository.findMostViewedByFreelancer(eq(8L), any())).thenReturn(new PageImpl<>(List.of(acceptedRecent)));
        when(applicationRepository.count()).thenReturn(3L);
        when(offerRepository.count()).thenReturn(2L);

        DashboardStatsResponse stats = dashboardService.getFreelancerDashboardStats(8L);
        assertThat(stats.getActiveContracts()).isEqualTo(1);
        assertThat(stats.getPendingApplications()).isEqualTo(1);
        assertThat(stats.getActiveOffers()).isEqualTo(1);

        DashboardStatsResponse period = dashboardService.getFreelancerDashboardStatsForPeriod(
                8L, LocalDate.now().minusDays(10), LocalDate.now()
        );
        assertThat(period.getTotalProjectsPosted()).isEqualTo(1);

        assertThat(dashboardService.getRevenueByMonth(8L, LocalDate.now().getYear()).values())
                .anyMatch(v -> v.compareTo(BigDecimal.ZERO) >= 0);
        assertThat(dashboardService.getApplicationTrendByFreelancer(8L, 30)).isNotNull();
        assertThat(dashboardService.getTopOfferIdsByViews(8L, 5)).contains(1L);
        assertThat(dashboardService.getGlobalStatsForAdmin()).containsEntry("totalOffers", 2L);
    }
}
