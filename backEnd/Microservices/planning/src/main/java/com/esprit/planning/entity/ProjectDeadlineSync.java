package com.esprit.planning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks project deadlines that have been synced to Google Calendar
 * so we do not create duplicate events.
 */
@Entity
@Table(name = "project_deadline_sync", uniqueConstraints = @UniqueConstraint(columnNames = "projectId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDeadlineSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long projectId;

    @Column(nullable = false, length = 512)
    private String calendarEventId;

    @Column(nullable = false)
    private LocalDateTime syncedAt;
}
