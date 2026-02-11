package org.example.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private Integer activeContracts;
    private Integer activeContractsThisWeek;
    private BigDecimal totalSpentLast30Days;
    private Integer totalProjectsPosted;
    private Integer pendingApplications;
    private Integer activeOffers;
}