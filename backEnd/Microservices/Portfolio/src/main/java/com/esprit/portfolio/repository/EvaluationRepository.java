package com.esprit.portfolio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.esprit.portfolio.entity.Evaluation;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByFreelancerId(Long freelancerId);
    List<Evaluation> findBySkillId(Long skillId);
    java.util.Optional<Evaluation> findByFreelancerIdAndSkillId(Long freelancerId, Long skillId);

    /**
     * Admin: per skill, total evaluation attempts and how many passed.
     * Returns Object[] { skillName(String), totalAttempts(Long), passedCount(Long) }.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT e.skill.name, COUNT(e), " +
        "SUM(CASE WHEN e.passed = TRUE THEN 1 ELSE 0 END) " +
        "FROM Evaluation e GROUP BY e.skill.name ORDER BY COUNT(e) DESC")
    List<Object[]> countEvaluationsBySkill();
}
