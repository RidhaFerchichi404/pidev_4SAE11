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
}
