package com.esprit.planning.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "progress_update")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A progress update for a project (submitted by a freelancer)")
public class ProgressUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @Column(nullable = true)
    @Schema(description = "Contract ID (null while contract microservice is not available)", example = "1")
    private Long contractId;

    @Column(nullable = false)
    @Schema(description = "Freelancer ID who submitted the update", example = "10")
    private Long freelancerId;

    @Column(nullable = false)
    @Schema(description = "Title of the update", example = "Backend API completed")
    private String title;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Detailed description of progress", example = "Implemented all REST endpoints.")
    private String description;

    @Column(nullable = false)
    @Schema(description = "Progress percentage (0-100)", example = "75")
    private Integer progressPercentage;

    @Column(nullable = false, updatable = false)
    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "progressUpdate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private List<ProgressComment> comments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
