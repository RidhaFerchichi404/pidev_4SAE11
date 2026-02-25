package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Summary of a progress update for list/pagination responses")
public class ProgressUpdateSummaryDto {

    @Schema(description = "Progress update ID")
    private Long id;

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Freelancer ID")
    private Long freelancerId;

    @Schema(description = "Update title")
    private String title;

    @Schema(description = "Progress percentage (0-100)")
    private Integer progressPercentage;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Number of comments on this update")
    private long commentCount;
}
