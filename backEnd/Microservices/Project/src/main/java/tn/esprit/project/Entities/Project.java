package tn.esprit.project.Entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tn.esprit.project.Entities.Enums.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    Long clientId;
    String title;
    String description;
    BigDecimal budget;
    LocalDateTime deadline;
    ProjectStatus status;
    String category;
    List<String> skillsRequiered;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "project",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProjectApplication> applications = new ArrayList<>();




}
