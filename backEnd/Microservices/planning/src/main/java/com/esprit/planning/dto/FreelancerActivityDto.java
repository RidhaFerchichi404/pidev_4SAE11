package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Freelancer activity ranking (updates and comments on their updates)")
public class FreelancerActivityDto {

    @Schema(description = "Freelancer ID", example = "10")
    private Long freelancerId;

    @Schema(description = "Number of progress updates submitted")
    private long updateCount;

    @Schema(description = "Number of comments on this freelancer's updates")
    private long commentCount;
}
