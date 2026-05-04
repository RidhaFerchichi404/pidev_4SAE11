package tn.esprit.project.Client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Mirrors task service {@code TaskStatsDto} JSON for Feign.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStatsFeignDto {

    private long totalTasks;
    private long doneCount;
    private long inProgressCount;
    private long overdueCount;
    private double completionPercentage;
}
