package tn.esprit.project.Dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectApplicationRequest {

    @NotNull
    private Long projectId;

    @NotNull
    private Long freelanceId;

    @NotBlank
    @Size(min = 20)
    private String coverLetter;

    @NotNull
    @DecimalMin(value="0.0", inclusive=false)
    private BigDecimal proposedPrice;

    @NotNull
    @Min(1)
    private Integer proposedDuration;
}
