package org.example.contract.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conflicts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    private Long raisedById;

    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_url", columnDefinition = "TEXT")
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    private ConflictStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime resolvedAt;
    
    @Column(columnDefinition = "TEXT")
    private String resolution;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ConflictStatus.OPEN;
        }
    }
}
