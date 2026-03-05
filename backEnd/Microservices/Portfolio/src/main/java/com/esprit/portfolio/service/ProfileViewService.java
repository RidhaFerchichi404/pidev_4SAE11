package com.esprit.portfolio.service;

import com.esprit.portfolio.entity.ProfileView;
import com.esprit.portfolio.repository.ProfileViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esprit.portfolio.dto.DailyViewStat;
import com.esprit.portfolio.dto.ProfileViewItem;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileViewService {

    private final ProfileViewRepository profileViewRepository;

    /**
     * Records a profile view, deduplicated per (profileUserId, viewerId, day).
     * Self-views are silently ignored.
     */
    @Transactional
    public void recordView(Long profileUserId, Long viewerId) {
        // Ignore self-views
        if (viewerId != null && viewerId.equals(profileUserId)) return;

        LocalDate today = LocalDate.now();
        boolean alreadySeen;

        if (viewerId != null) {
            alreadySeen = profileViewRepository
                    .existsByProfileUserIdAndViewerIdAndViewDate(profileUserId, viewerId, today);
        } else {
            alreadySeen = profileViewRepository.existsAnonymousViewToday(profileUserId, today);
        }

        if (!alreadySeen) {
            ProfileView view = ProfileView.builder()
                    .profileUserId(profileUserId)
                    .viewerId(viewerId)
                    .viewDate(today)
                    .build();
            profileViewRepository.save(view);
        }
    }

    /** Total all-time views for a freelancer's profile. */
    @Transactional(readOnly = true)
    public long getTotalViewCount(Long profileUserId) {
        return profileViewRepository.countByProfileUserId(profileUserId);
    }

    /** Recent viewers (up to `limit`), newest first. */
    @Transactional(readOnly = true)
    public List<ProfileViewItem> getRecentViewers(Long profileUserId, int limit) {
        return profileViewRepository
                .findRecentByProfileUserId(profileUserId, PageRequest.of(0, limit))
                .stream()
                .map(pv -> new ProfileViewItem(pv.getViewerId(), pv.getViewedAt()))
                .toList();
    }

    /** Views in the last 7 days. */
    @Transactional(readOnly = true)
    public long getThisWeekViewCount(Long profileUserId) {
        return profileViewRepository.countSinceDate(profileUserId, LocalDate.now().minusDays(7));
    }

    /** Views in the 7-day window before last week (days 8–14 ago). */
    @Transactional(readOnly = true)
    public long getLastWeekViewCount(Long profileUserId) {
        LocalDate end   = LocalDate.now().minusDays(7);
        LocalDate start = LocalDate.now().minusDays(14);
        return profileViewRepository.countBetweenDates(profileUserId, start, end);
    }

    /** Daily view counts for the last `days` days (for chart). */
    @Transactional(readOnly = true)
    public List<DailyViewStat> getDailyStats(Long profileUserId, int days) {
        LocalDate since = LocalDate.now().minusDays(days - 1);
        return profileViewRepository.getDailyStats(profileUserId, since)
                .stream()
                .map(row -> new DailyViewStat((LocalDate) row[0], (Long) row[1]))
                .toList();
    }
}
