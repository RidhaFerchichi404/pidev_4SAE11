package tn.esprit.project.Dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectClientSegmentResponse {

    private Long clientId;
    private int segmentId;
    private String segmentLabel;
    private double confidence;
    private List<String> reasons;
    private boolean available;
    private String message;
}
