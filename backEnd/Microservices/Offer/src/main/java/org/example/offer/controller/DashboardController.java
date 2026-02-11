package org.example.offer.controller;

import lombok.RequiredArgsConstructor;
import org.example.offer.dto.response.DashboardStatsResponse;
import org.example.offer.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard/freelancer/{freelancerId} - Statistiques du dashboard
     */
    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<DashboardStatsResponse> getFreelancerStats(
            @PathVariable Long freelancerId) {
        DashboardStatsResponse stats = dashboardService.getFreelancerDashboardStats(freelancerId);
        return ResponseEntity.ok(stats);
    }
}