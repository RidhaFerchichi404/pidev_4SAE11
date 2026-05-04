package tn.esprit.project.Client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewFeignDto {

    private Long id;
    private Integer rating;
    private Long projectId;
}
