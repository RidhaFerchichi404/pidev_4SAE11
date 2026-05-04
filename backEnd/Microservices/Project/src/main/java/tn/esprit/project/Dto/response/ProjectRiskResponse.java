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
public class ProjectRiskResponse {

    /** Estimated failure risk: (1 - P(success)) * 100, one decimal. */
    private double riskPercent;

    private double successProbability;

    /**
     * Short explanations from the ML service (counterfactual vs training defaults), when available.
     */
    private List<String> reasons;

    /** True if at least one optional source (task stats or reviews) was loaded successfully. */
    private boolean aggregatesUsed;

    /** False when ML is disabled or the inference service is unreachable. */
    private boolean available;

    private String message;
}
