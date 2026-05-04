package tn.esprit.project.Dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSegmentationOverviewResponse {

    private List<ProjectClientSegmentResponse> segments;
    private Map<String, Integer> summaryCounts;
    private boolean available;
    private String message;
}
