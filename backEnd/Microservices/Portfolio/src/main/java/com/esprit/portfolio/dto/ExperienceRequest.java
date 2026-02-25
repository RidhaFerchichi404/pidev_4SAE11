package com.esprit.portfolio.dto;

import java.time.LocalDate;
import java.util.List;

import com.esprit.portfolio.entity.ExperienceType;

import lombok.Data;

@Data
public class ExperienceRequest {
    private Long userId;
    private String title;
    private ExperienceType type;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String companyOrClientName;
    private List<String> keyTasks;
    private List<String> skillNames;
}
