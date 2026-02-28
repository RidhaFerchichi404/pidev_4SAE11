package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Calendar event (from Google Calendar when integration is enabled)")
public class CalendarEventDto {

    @Schema(description = "Event ID", example = "abc123")
    private String id;

    @Schema(description = "Event title/summary", example = "Next progress update due – Backend API")
    private String summary;

    @Schema(description = "Start time")
    private LocalDateTime start;

    @Schema(description = "End time")
    private LocalDateTime end;

    @Schema(description = "Optional description")
    private String description;
}
