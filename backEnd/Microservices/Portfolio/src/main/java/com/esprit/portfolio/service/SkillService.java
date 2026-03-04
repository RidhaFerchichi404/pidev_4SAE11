package com.esprit.portfolio.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esprit.portfolio.dto.SkillDomainStatDto;
import com.esprit.portfolio.dto.SkillSuccessStatDto;
import com.esprit.portfolio.dto.SkillUsageStatDto;
import com.esprit.portfolio.entity.Domain;
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

    // ------------------------------------------------------------------ //
    //  Domain helpers
    // ------------------------------------------------------------------ //

    /** All pre-defined domains — used by the frontend to build checkboxes. */
    public List<Domain> getAllDomains() {
        return Arrays.asList(Domain.values());
    }

    // ------------------------------------------------------------------ //
    //  Read
    // ------------------------------------------------------------------ //

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

    // ------------------------------------------------------------------ //
    //  Create / Update
    // ------------------------------------------------------------------ //

    @Transactional
    public Skill create(Skill skill) {
        validateDomains(skill.getDomains());
        if (skillRepository.findByNameAndUserId(skill.getName(), skill.getUserId()).isPresent()) {
            throw new IllegalArgumentException("Skill '" + skill.getName() + "' already exists in your profile");
        }
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
        if (skillDetails.getDomains() != null && !skillDetails.getDomains().isEmpty()) {
            validateDomains(skillDetails.getDomains());
            skill.setDomains(skillDetails.getDomains());
        }
        if (skillDetails.getDescription() != null) skill.setDescription(skillDetails.getDescription());
        return skillRepository.save(skill);
    }

    /**
     * Freelancer operation: replace all domain checkboxes for a skill.
     * Expects at least one domain in the list.
     */
    @Transactional
    public Skill updateSkillDomains(Long skillId, List<Domain> newDomains) {
        validateDomains(newDomains);
        Skill skill = findById(skillId);
        skill.setDomains(newDomains);
        return skillRepository.save(skill);
    }

    // ------------------------------------------------------------------ //
    //  Delete
    // ------------------------------------------------------------------ //

    @Transactional
    public void delete(Long id) {
        findById(id); // validates existence — throws if not found

        // 1. Remove from Experiences (ManyToMany)
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

        // 4. Delete the skill (cascade removes skill_domains rows)
        skillRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ //
    //  Admin statistics
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public List<SkillDomainStatDto> getSkillStatsByDomain() {
        return skillRepository.countSkillsGroupedByDomain();
    }

    /**
     * Admin: how many distinct freelancers have each skill, sorted by popularity.
     * Percentage is relative to the total number of freelancers who have any skill.
     */
    @Transactional(readOnly = true)
    public List<SkillUsageStatDto> getSkillUsageStats() {
        List<Object[]> rows = skillRepository.countUsersBySkillName();
        Long totalUsers = skillRepository.countDistinctUsers();
        if (totalUsers == null || totalUsers == 0) return List.of();
        return rows.stream()
                .map(r -> new SkillUsageStatDto(
                        (String) r[0],
                        (Long) r[1],
                        Math.round(((Long) r[1]) * 1000.0 / totalUsers) / 10.0))
                .collect(Collectors.toList());
    }

    /**
     * Admin: per skill, total test attempts and success rate (% passed).
     */
    @Transactional(readOnly = true)
    public List<SkillSuccessStatDto> getSkillSuccessStats() {
        List<Object[]> rows = evaluationRepository.countEvaluationsBySkill();
        return rows.stream()
                .map(r -> {
                    Long total  = (Long) r[1];
                    Long passed = ((Number) r[2]).longValue();
                    double rate = total > 0 ? Math.round(passed * 1000.0 / total) / 10.0 : 0.0;
                    return new SkillSuccessStatDto((String) r[0], total, passed, rate);
                })
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    private void validateDomains(List<Domain> domains) {
        if (domains == null || domains.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one domain must be selected. Allowed values: "
                    + Arrays.toString(Domain.values()));
        }
    }
}
