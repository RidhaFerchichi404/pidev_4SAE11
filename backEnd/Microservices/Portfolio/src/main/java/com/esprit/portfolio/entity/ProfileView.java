package com.esprit.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Records each unique profile view (deduplicated per viewer per day).
 * Anonymous views (viewerId = null) are deduplicated per profileUserId per day.
 */
@Entity
@Table(
    name = "profile_views",
    indexes = {
        @Index(name = "idx_pv_profile_user", columnList = "profile_user_id"),
        @Index(name = "idx_pv_view_date",    columnList = "profile_user_id, view_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The freelancer whose profile was viewed. */
    @Column(name = "profile_user_id", nullable = false)
    private Long profileUserId;

    /** Who viewed the profile. Null = anonymous / unauthenticated visitor. */
    @Column(name = "viewer_id")
    private Long viewerId;

    /** Calendar date — used for per-day deduplication. */
    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;

    /** Exact timestamp of the first view this day. */
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    void onCreate() {
        if (viewedAt == null) viewedAt = LocalDateTime.now();
        if (viewDate  == null) viewDate  = LocalDate.now();
    }
}
