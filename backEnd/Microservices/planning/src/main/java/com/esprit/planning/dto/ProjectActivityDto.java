package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Project activity ranking by update count")
public class ProjectActivityDto {

    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @Schema(description = "Number of progress updates for this project")
    private long updateCount;
}
