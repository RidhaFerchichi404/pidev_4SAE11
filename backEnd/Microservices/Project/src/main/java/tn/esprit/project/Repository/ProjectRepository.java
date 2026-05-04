package tn.esprit.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.project.Entities.Enums.ApplicationStatus;
import tn.esprit.project.Entities.Enums.ProjectStatus;
import tn.esprit.project.Entities.Project;

import java.math.BigDecimal;
import java.util.List;


@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByClientId(Long clientId);

    /** Projets du client pour lesquels le freelancer a au moins une candidature. */
    @Query("SELECT DISTINCT p FROM Project p JOIN p.applications a WHERE p.clientId = :clientId AND a.freelanceId = :freelancerId")
    List<Project> findJointProjects(@Param("clientId") Long clientId, @Param("freelancerId") Long freelancerId);

    /** Projets pour lesquels ce freelancer a une candidature acceptée (missions qu'il exécute). */
    @Query("SELECT DISTINCT p FROM Project p JOIN p.applications a WHERE a.freelanceId = :freelancerId AND a.status = :accepted")
    List<Project> findProjectsWhereFreelancerAccepted(
            @Param("freelancerId") Long freelancerId,
            @Param("accepted") ApplicationStatus accepted);

    /**
     * Projets publiés par ce client où au moins une candidature a été acceptée
     * (le client a choisi un freelancer sur l'annonce).
     */
    @Query("SELECT DISTINCT p FROM Project p JOIN p.applications a WHERE p.clientId = :clientId AND a.status = :accepted")
    List<Project> findProjectsWhereClientAcceptedAFreelancer(
            @Param("clientId") Long clientId,
            @Param("accepted") ApplicationStatus accepted);

    List<Project> findByStatus(ProjectStatus status);
    long countByClientId(Long clientId);

    @Query("SELECT COALESCE(AVG(p.budget), 0) FROM Project p WHERE p.clientId = :clientId")
    BigDecimal averageBudgetByClientId(@Param("clientId") Long clientId);

    @Query("SELECT DISTINCT p.clientId FROM Project p WHERE p.clientId IS NOT NULL")
    List<Long> findDistinctClientIds();
}
