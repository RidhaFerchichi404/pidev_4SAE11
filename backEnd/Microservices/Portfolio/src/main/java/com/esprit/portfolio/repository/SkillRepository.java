package com.esprit.portfolio.repository;

import com.esprit.portfolio.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByUserId(Long userId);
    // Find skill by name AND userId (uniqueness per user)
    Optional<Skill> findByNameAndUserId(String name, Long userId);
}
