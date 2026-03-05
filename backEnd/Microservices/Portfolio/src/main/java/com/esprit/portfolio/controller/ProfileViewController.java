package com.esprit.portfolio.controller;

import com.esprit.portfolio.dto.DailyViewStat;
import com.esprit.portfolio.dto.ProfileViewItem;
import com.esprit.portfolio.dto.ProfileViewRequest;
import com.esprit.portfolio.dto.ProfileViewStats;
import com.esprit.portfolio.service.ProfileViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/views")
@RequiredArgsConstructor
public class ProfileViewController {

    private final ProfileViewService profileViewService;

    /**
     * POST /api/views/record
     * Records a profile view (idempotent: one view per viewer per day).
     * Body: { "profileUserId": 42, "viewerId": 7 }   (viewerId optional — null = anonymous)
     */
    @PostMapping("/record")
    public ResponseEntity<Void> recordView(@RequestBody ProfileViewRequest request) {
        profileViewService.recordView(request.getProfileUserId(), request.getViewerId());
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/views/user/{userId}/count
     * Returns the total all-time view count for a freelancer's profile.
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> getTotalCount(@PathVariable Long userId) {
        return ResponseEntity.ok(profileViewService.getTotalViewCount(userId));
    }

    /**
     * GET /api/views/user/{userId}/recent?limit=10
     * Returns recent viewers (viewerId + timestamp), newest first.
     */
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<ProfileViewItem>> getRecentViewers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(profileViewService.getRecentViewers(userId, limit));
    }

    /**
     * GET /api/views/user/{userId}/stats
     * Returns total + this-week view counts for the freelancer's profile analytics.
     */
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<ProfileViewStats> getStats(@PathVariable Long userId) {
        long total    = profileViewService.getTotalViewCount(userId);
        long week     = profileViewService.getThisWeekViewCount(userId);
        long lastWeek = profileViewService.getLastWeekViewCount(userId);
        return ResponseEntity.ok(new ProfileViewStats(total, week, lastWeek));
    }

    /**
     * GET /api/views/user/{userId}/daily?days=30
     * Returns per-day view counts for the analytics chart.
     */
    @GetMapping("/user/{userId}/daily")
    public ResponseEntity<List<DailyViewStat>> getDailyStats(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(profileViewService.getDailyStats(userId, days));
    }
}
