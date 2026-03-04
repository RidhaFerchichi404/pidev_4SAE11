package com.esprit.portfolio.repository;

import com.esprit.portfolio.dto.SkillDomainStatDto;
import com.esprit.portfolio.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByUserId(Long userId);

    Optional<Skill> findByNameAndUserId(String name, Long userId);

    /**
     * Admin: distinct skills grouped by domain.
     */
    @Query("SELECT new com.esprit.portfolio.dto.SkillDomainStatDto(d, COUNT(DISTINCT s)) " +
           "FROM Skill s JOIN s.domains d GROUP BY d ORDER BY COUNT(DISTINCT s) DESC")
    List<SkillDomainStatDto> countSkillsGroupedByDomain();

    /**
     * Admin: for each skill name, count how many distinct freelancers have it.
     * Returns Object[] { skillName(String), userCount(Long) }.
     */
    @Query("SELECT s.name, COUNT(DISTINCT s.userId) FROM Skill s GROUP BY s.name ORDER BY COUNT(DISTINCT s.userId) DESC")
    List<Object[]> countUsersBySkillName();

    /**
     * Total number of distinct freelancers who have at least one skill.
     */
    @Query("SELECT COUNT(DISTINCT s.userId) FROM Skill s")
    Long countDistinctUsers();
}
