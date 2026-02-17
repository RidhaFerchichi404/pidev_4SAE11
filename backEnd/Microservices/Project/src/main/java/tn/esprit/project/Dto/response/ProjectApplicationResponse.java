package tn.esprit.project.Dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectApplicationResponse {

    private Long id;
    private Long projectId;
    private Long freelanceId;
    private BigDecimal proposedPrice;
    private Integer proposedDuration;
    private String status;
    private LocalDateTime appliedAt;
}