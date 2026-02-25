package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Project with no recent progress update (stalled)")
public class StalledProjectDto {

    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @Schema(description = "Timestamp of the last progress update")
    private LocalDateTime lastUpdateAt;

    @Schema(description = "Progress percentage at the last update (0-100)", example = "45")
    private Integer lastProgressPercentage;
}
