package tn.esprit.project.Dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequest {

    @NotNull
    private Long clientId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin(value="0.0", inclusive=false)
    private BigDecimal budget;

    @NotNull
    @Future
    private LocalDateTime deadline;

    @NotBlank
    private String category;

    @NotBlank
    private String skillsRequiered;
}
