package com.esprit.portfolio.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esprit.portfolio.entity.Evaluation;
import com.esprit.portfolio.entity.EvaluationTest;
import com.esprit.portfolio.entity.Experience;
import com.esprit.portfolio.entity.Skill;
import com.esprit.portfolio.repository.EvaluationRepository;
import com.esprit.portfolio.repository.EvaluationTestRepository;
import com.esprit.portfolio.repository.ExperienceRepository;
import com.esprit.portfolio.repository.SkillRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final EvaluationTestRepository evaluationTestRepository;
    private final EvaluationRepository evaluationRepository;

    @Transactional(readOnly = true)
    public List<Skill> findAll() {
        return skillRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Skill> findAllByUserId(Long userId) {
        return skillRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Skill findById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + id));
    }

    @Transactional
    public Skill create(Skill skill) {
        // Check uniqueness per user
        if (skillRepository.findByNameAndUserId(skill.getName(), skill.getUserId()).isPresent()) {
            throw new IllegalArgumentException("Skill '" + skill.getName() + "' already exists in your profile");
        }
        // Timestamps are automatically set by @PrePersist
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill update(Long id, Skill skillDetails) {
        Skill skill = findById(id);
        if (skillDetails.getName() != null && !skillDetails.getName().equals(skill.getName()) &&
                skillRepository.findByNameAndUserId(skillDetails.getName(), skill.getUserId()).isPresent()) {
            throw new RuntimeException("Skill already exists for user: " + skillDetails.getName());
        }
        if (skillDetails.getName() != null) skill.setName(skillDetails.getName());
        if (skillDetails.getDomain() != null) skill.setDomain(skillDetails.getDomain());
        if (skillDetails.getDescription() != null) skill.setDescription(skillDetails.getDescription());
        return skillRepository.save(skill);
    }

    @Transactional
    public void delete(Long id) {
        Skill skill = findById(id);
        
        // 1. Remove from Experiences (if linked via ManyToMany)
        List<Experience> experiences = experienceRepository.findBySkills_Id(id);
        for (Experience exp : experiences) {
            exp.getSkills().removeIf(s -> s.getId().equals(id));
            experienceRepository.save(exp);
        }

        // 2. Delete related EvaluationTests
        List<EvaluationTest> tests = evaluationTestRepository.findBySkillId(id);
        evaluationTestRepository.deleteAll(tests);

        // 3. Delete related Evaluations
        List<Evaluation> evaluations = evaluationRepository.findBySkillId(id);
        evaluationRepository.deleteAll(evaluations);

        // 4. Delete the Skill
        skillRepository.deleteById(id);
    }
}
