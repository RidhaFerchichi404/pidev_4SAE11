package com.example.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "review_id", nullable = false, insertable = false, updatable = false)
    private Long reviewId;
    
    @Column(name = "respondent_id", nullable = false)
    private Long respondentId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    @JsonIgnore
    private Review review;
    
    @PrePersist
    protected void onCreate() {
        respondedAt = LocalDateTime.now();
    }
}
