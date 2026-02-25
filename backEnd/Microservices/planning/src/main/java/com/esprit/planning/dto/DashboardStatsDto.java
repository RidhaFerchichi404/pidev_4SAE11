package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dashboard-wide progress statistics")
public class DashboardStatsDto {

    @Schema(description = "Total number of progress updates", example = "150")
    private long totalUpdates;

    @Schema(description = "Total number of comments on updates", example = "80")
    private long totalComments;

    @Schema(description = "Average progress percentage across all updates", example = "55.5")
    private Double averageProgressPercentage;

    @Schema(description = "Number of distinct projects with updates", example = "25")
    private long distinctProjectCount;

    @Schema(description = "Number of distinct freelancers who submitted updates", example = "40")
    private long distinctFreelancerCount;
}
