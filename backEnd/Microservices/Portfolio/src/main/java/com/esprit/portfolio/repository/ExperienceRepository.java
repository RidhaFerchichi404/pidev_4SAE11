package com.esprit.portfolio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.esprit.portfolio.dto.ExperienceDomainStatDto;
import com.esprit.portfolio.entity.Experience;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    List<Experience> findByUserIdOrderByStartDateDesc(Long userId);

    List<Experience> findBySkills_Id(Long skillId);

    /**
     * Admin dashboard: total experiences grouped by domain.
     * Only counts experiences that have a domain set (IS NOT NULL).
     */
    @Query("SELECT new com.esprit.portfolio.dto.ExperienceDomainStatDto(e.domain, COUNT(e)) " +
           "FROM Experience e WHERE e.domain IS NOT NULL GROUP BY e.domain ORDER BY COUNT(e) DESC")
    List<ExperienceDomainStatDto> countExperiencesGroupedByDomain();
}
