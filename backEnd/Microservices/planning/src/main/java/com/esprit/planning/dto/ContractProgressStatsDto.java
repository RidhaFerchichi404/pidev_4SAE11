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
@Schema(description = "Progress statistics for a contract")
public class ContractProgressStatsDto {

    @Schema(description = "Contract ID", example = "1")
    private Long contractId;

    @Schema(description = "Number of progress updates", example = "10")
    private long updateCount;

    @Schema(description = "Total comments on this contract's updates", example = "4")
    private long commentCount;

    @Schema(description = "Current progress percentage (latest or max)", example = "75")
    private Integer currentProgressPercentage;

    @Schema(description = "Timestamp of the first update")
    private LocalDateTime firstUpdateAt;

    @Schema(description = "Timestamp of the most recent update")
    private LocalDateTime lastUpdateAt;
}
