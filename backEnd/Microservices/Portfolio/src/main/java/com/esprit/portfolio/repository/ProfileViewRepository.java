package com.esprit.portfolio.repository;

import com.esprit.portfolio.entity.ProfileView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProfileViewRepository extends JpaRepository<ProfileView, Long> {

    /** Total all-time views for a profile. */
    long countByProfileUserId(Long profileUserId);

    /** Views since a given date (for "this week" count). */
    @Query("SELECT COUNT(pv) FROM ProfileView pv " +
           "WHERE pv.profileUserId = :userId AND pv.viewDate >= :startDate")
    long countSinceDate(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);

    /** Views between two dates (exclusive end) — for "last week" comparison. */
    @Query("SELECT COUNT(pv) FROM ProfileView pv " +
           "WHERE pv.profileUserId = :userId AND pv.viewDate >= :startDate AND pv.viewDate < :endDate")
    long countBetweenDates(@Param("userId") Long userId,
                           @Param("startDate") LocalDate startDate,
                           @Param("endDate") LocalDate endDate);

    /** Dedup check for authenticated viewers. */
    boolean existsByProfileUserIdAndViewerIdAndViewDate(
            Long profileUserId, Long viewerId, LocalDate viewDate);

    /** Dedup check for anonymous viewers (viewerId IS NULL). */
    @Query("SELECT COUNT(pv) > 0 FROM ProfileView pv " +
           "WHERE pv.profileUserId = :userId AND pv.viewerId IS NULL AND pv.viewDate = :date")
    boolean existsAnonymousViewToday(
            @Param("userId") Long userId, @Param("date") LocalDate date);

    /** Recent views for a profile, newest first, limited to top N. */
    @Query("SELECT pv FROM ProfileView pv " +
           "WHERE pv.profileUserId = :userId " +
           "ORDER BY pv.viewedAt DESC")
    List<ProfileView> findRecentByProfileUserId(
            @Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    /** Daily view counts over the last N days — for the analytics chart. */
    @Query("SELECT pv.viewDate, COUNT(pv) FROM ProfileView pv " +
           "WHERE pv.profileUserId = :userId AND pv.viewDate >= :startDate " +
           "GROUP BY pv.viewDate ORDER BY pv.viewDate ASC")
    List<Object[]> getDailyStats(
            @Param("userId") Long userId, @Param("startDate") LocalDate startDate);
}
