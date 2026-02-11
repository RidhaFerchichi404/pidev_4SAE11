package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import org.example.offer.dto.response.DashboardStatsResponse;
import org.example.offer.entity.ApplicationStatus;
import org.example.offer.entity.OfferStatus;
import org.example.offer.repository.OfferApplicationRepository;
import org.example.offer.repository.OfferRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OfferRepository offerRepository;
    private final OfferApplicationRepository applicationRepository;

    /**
     * Récupérer les statistiques du dashboard pour un freelancer
     */
    public DashboardStatsResponse getFreelancerDashboardStats(Long freelancerId) {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        // Active Contracts (offres acceptées)
        Integer activeContracts = offerRepository
                .findByFreelancerIdAndStatus(freelancerId, OfferStatus.ACCEPTED)
                .size();
        stats.setActiveContracts(activeContracts);

        // Active Contracts this week
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        long activeThisWeek = offerRepository
                .findByFreelancerId(freelancerId)
                .stream()
                .filter(offer -> offer.getCreatedAt().isAfter(oneWeekAgo))
                .filter(offer -> offer.getStatus() == OfferStatus.ACCEPTED)
                .count();
        stats.setActiveContractsThisWeek((int) activeThisWeek);

        // Total Spent Last 30 Days (somme des prix des offres acceptées)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        BigDecimal totalSpent = offerRepository
                .findByFreelancerId(freelancerId)
                .stream()
                .filter(offer -> offer.getCreatedAt().isAfter(thirtyDaysAgo))
                .filter(offer -> offer.getStatus() == OfferStatus.ACCEPTED)
                .map(offer -> offer.getPrice() != null ? offer.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalSpentLast30Days(totalSpent);

        // Total Projects Posted (lifetime)
        Integer totalProjects = offerRepository
                .findByFreelancerId(freelancerId)
                .size();
        stats.setTotalProjectsPosted(totalProjects);

        // Pending Applications
        long pendingApps = applicationRepository
                .findByStatus(ApplicationStatus.PENDING)
                .stream()
                .filter(app -> app.getOffer().getFreelancerId().equals(freelancerId))
                .count();
        stats.setPendingApplications((int) pendingApps);

        // Active Offers
        Integer activeOffers = offerRepository
                .findByFreelancerIdAndStatus(freelancerId, OfferStatus.AVAILABLE)
                .size();
        stats.setActiveOffers(activeOffers);

        return stats;
    }
}