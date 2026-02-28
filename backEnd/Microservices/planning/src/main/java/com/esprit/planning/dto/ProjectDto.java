package com.esprit.planning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Project info from Project microservice (for clientId lookup and deadline sync to calendar). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private Long id;
    private Long clientId;
    private String title;
    private LocalDateTime deadline;
}
