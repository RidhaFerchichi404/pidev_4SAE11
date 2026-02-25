package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Progress statistics for a freelancer")
public class FreelancerProgressStatsDto {

    @Schema(description = "Freelancer ID", example = "10")
    private Long freelancerId;

    @Schema(description = "Total number of progress updates", example = "25")
    private long totalUpdates;

    @Schema(description = "Total comments on this freelancer's updates", example = "12")
    private long totalComments;

    @Schema(description = "Average progress percentage across all updates", example = "62.5")
    private Double averageProgressPercentage;

    @Schema(description = "Timestamp of the most recent update")
    private LocalDateTime lastUpdateAt;

    @Schema(description = "Number of updates in the last 30 days (activity score)", example = "5")
    private long updatesLast30Days;
}
