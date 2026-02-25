package com.esprit.portfolio.dto;

import lombok.Data;
import java.util.List;

@Data
public class TestSubmission {
    private Long testId;
    private Long freelancerId;
    private List<AnswerSubmission> answers;
}
