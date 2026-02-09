package tn.esprit.project.Entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tn.esprit.project.Entities.Enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", insertable = false, updatable = false)
    private Long projectId;
    Long freelanceId;
    String coverLetter;
    BigDecimal proposedPrice;
    Integer proposedDuration;
    ApplicationStatus status;
    LocalDateTime appliedAt;
    LocalDateTime respondedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;


}
