package com.esprit.portfolio.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esprit.portfolio.dto.ExperienceDomainStatDto;
import com.esprit.portfolio.dto.ExperienceRequest;
import com.esprit.portfolio.entity.Domain;
import com.esprit.portfolio.entity.Experience;
import com.esprit.portfolio.entity.Skill;
import com.esprit.portfolio.repository.ExperienceRepository;
import com.esprit.portfolio.repository.SkillRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<Experience> getAllExperiences() {
        return experienceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Experience> getExperienceById(Long id) {
        return experienceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Experience> getExperiencesByUserId(Long userId) {
        return experienceRepository.findByUserIdOrderByStartDateDesc(userId);
    }

    @Transactional
    public Experience createExperience(ExperienceRequest request) {
        List<Skill> skills = resolveAndLinkSkills(request.getUserId(), request.getSkillNames());

        Experience experience = Experience.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .type(request.getType())
                .domain(request.getDomain())   // may be null — domain is optional on Experience
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .companyOrClientName(request.getCompanyOrClientName())
                .keyTasks(request.getKeyTasks())
                .skills(skills)
                .build();

        return experienceRepository.save(experience);
    }

    @Transactional
    public Experience updateExperience(Long id, ExperienceRequest request) {
        return experienceRepository.findById(id).map(experience -> {
            experience.setTitle(request.getTitle());
            experience.setType(request.getType());
            if (request.getDomain() != null) experience.setDomain(request.getDomain());
            experience.setDescription(request.getDescription());
            experience.setStartDate(request.getStartDate());
            experience.setEndDate(request.getEndDate());
            experience.setCompanyOrClientName(request.getCompanyOrClientName());

            if (request.getKeyTasks() != null) {
                experience.setKeyTasks(request.getKeyTasks());
            }
            if (request.getSkillNames() != null) {
                List<Skill> skills = resolveAndLinkSkills(experience.getUserId(), request.getSkillNames());
                experience.setSkills(skills);
            }

            return experienceRepository.save(experience);
        }).orElseThrow(() -> new RuntimeException("Experience not found with id " + id));
    }

    @Transactional
    public void deleteExperience(Long id) {
        experienceRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ //
    //  Admin statistics
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public List<ExperienceDomainStatDto> getExperienceStatsByDomain() {
        return experienceRepository.countExperiencesGroupedByDomain();
    }

    // ------------------------------------------------------------------ //
    //  Private helpers
    // ------------------------------------------------------------------ //

    private List<Skill> resolveAndLinkSkills(Long userId, List<String> skillNames) {
        List<Skill> skills = new ArrayList<>();
        if (skillNames == null) return skills;

        for (String name : skillNames) {
            Optional<Skill> existingSkill = skillRepository.findByNameAndUserId(name, userId);

            if (existingSkill.isPresent()) {
                skills.add(existingSkill.get());
            } else {
                // Auto-create skill with a neutral default domain
                Skill newSkill = Skill.builder()
                        .name(name)
                        .userId(userId)
                        .domains(new java.util.ArrayList<>(List.of(Domain.OTHER))) // default — freelancer can update via checkboxes
                        .description("Added via Experience")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                skills.add(skillRepository.save(newSkill));
            }
        }
        return skills;
    }
}
