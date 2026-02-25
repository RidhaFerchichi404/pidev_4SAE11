package com.esprit.portfolio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.esprit.portfolio.entity.EvaluationTest;

@Repository
public interface EvaluationTestRepository extends JpaRepository<EvaluationTest, Long> {
    List<EvaluationTest> findBySkillId(Long skillId);
}
