package tn.esprit.project.Dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;
    private Long clientId;
    private String title;
    private String description;
    private BigDecimal budget;
    private LocalDateTime deadline;
    private String status;
    private String category;
}
