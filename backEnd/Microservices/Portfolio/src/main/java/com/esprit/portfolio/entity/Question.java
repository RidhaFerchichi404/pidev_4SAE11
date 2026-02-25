package com.esprit.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private String questionText;
    @Column(length = 1000)
    private String options; // Comma separated or JSON
    private String correctOption;
    private Integer points;
}
