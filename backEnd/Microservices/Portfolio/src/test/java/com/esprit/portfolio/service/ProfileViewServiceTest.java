package com.esprit.portfolio.service;

import com.esprit.portfolio.dto.DailyViewStat;
import com.esprit.portfolio.dto.ProfileViewItem;
import com.esprit.portfolio.entity.ProfileView;
import com.esprit.portfolio.repository.ProfileViewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileViewServiceTest {

    @Mock
    private ProfileViewRepository profileViewRepository;

    @InjectMocks
    private ProfileViewService service;

    @Test
    void recordView_ignoresSelfView() {
        service.recordView(10L, 10L);
        verify(profileViewRepository, never()).save(org.mockito.ArgumentMatchers.any(ProfileView.class));
    }

    @Test
    void recordView_savesForAuthenticatedViewerWhenNotSeen() {
        when(profileViewRepository.existsByProfileUserIdAndViewerIdAndViewDate(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(20L),
            org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(false);

        service.recordView(10L, 20L);

        ArgumentCaptor<ProfileView> captor = ArgumentCaptor.forClass(ProfileView.class);
        verify(profileViewRepository).save(captor.capture());
        assertThat(captor.getValue().getProfileUserId()).isEqualTo(10L);
        assertThat(captor.getValue().getViewerId()).isEqualTo(20L);
    }

    @Test
    void recordView_doesNotSaveAnonymousWhenAlreadySeenToday() {
        when(profileViewRepository.existsAnonymousViewToday(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(true);

        service.recordView(10L, null);

        verify(profileViewRepository, never()).save(org.mockito.ArgumentMatchers.any(ProfileView.class));
    }

    @Test
    void statsAndRecentViewers_delegateAndMapCorrectly() {
        ProfileView view = ProfileView.builder()
            .profileUserId(10L)
            .viewerId(30L)
            .viewDate(LocalDate.now())
            .viewedAt(LocalDateTime.now())
            .build();

        when(profileViewRepository.countByProfileUserId(10L)).thenReturn(15L);
        when(profileViewRepository.countSinceDate(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any(LocalDate.class)))
            .thenReturn(7L);
        when(profileViewRepository.countBetweenDates(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.any(LocalDate.class),
            org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(4L);
        when(profileViewRepository.findRecentByProfileUserId(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any(Pageable.class)))
            .thenReturn(List.of(view));
        List<Object[]> dailyRows = new java.util.ArrayList<>();
        dailyRows.add(new Object[]{LocalDate.now(), 3L});
        when(profileViewRepository.getDailyStats(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any(LocalDate.class)))
            .thenReturn(dailyRows);

        assertThat(service.getTotalViewCount(10L)).isEqualTo(15L);
        assertThat(service.getThisWeekViewCount(10L)).isEqualTo(7L);
        assertThat(service.getLastWeekViewCount(10L)).isEqualTo(4L);

        List<ProfileViewItem> recent = service.getRecentViewers(10L, 5);
        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).getViewerId()).isEqualTo(30L);

        List<DailyViewStat> daily = service.getDailyStats(10L, 7);
        assertThat(daily).hasSize(1);
        assertThat(daily.get(0).getCount()).isEqualTo(3L);
    }
}
