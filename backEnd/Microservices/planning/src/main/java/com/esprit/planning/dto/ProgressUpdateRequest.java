package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating or updating a progress update")
public class ProgressUpdateRequest {

    @Schema(description = "ID of the project", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    @Schema(description = "ID of the contract (optional; null if no contract yet)", example = "1", nullable = true)
    private Long contractId;

    @Schema(description = "ID of the freelancer who submitted the update", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long freelancerId;

    @Schema(description = "Short title of the progress update", example = "Backend API completed", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Detailed description of the progress", example = "Implemented all REST endpoints for user and project management.")
    private String description;

    @Schema(description = "Progress percentage (0-100)", example = "75", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "100")
    private Integer progressPercentage;

    @Schema(description = "Optional: next progress update due date (synced to Google Calendar when enabled)", example = "2025-03-15T14:00:00")
    private LocalDateTime nextUpdateDue;
}
