package com.esprit.portfolio.service;

import com.esprit.portfolio.dto.ExperienceDomainStatDto;
import com.esprit.portfolio.dto.ExperienceRequest;
import com.esprit.portfolio.entity.Domain;
import com.esprit.portfolio.entity.Experience;
import com.esprit.portfolio.entity.ExperienceType;
import com.esprit.portfolio.entity.Skill;
import com.esprit.portfolio.repository.ExperienceRepository;
import com.esprit.portfolio.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperienceServiceTest {

    @Mock
    private ExperienceRepository experienceRepository;
    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private ExperienceService service;

    @Test
    void createExperience_reusesExistingSkillAndCreatesMissingOne() {
        ExperienceRequest request = baseRequest();
        request.setSkillNames(List.of("Java", "K8s"));

        Skill existing = Skill.builder().id(1L).name("Java").userId(10L).build();
        Skill created = Skill.builder().id(2L).name("K8s").userId(10L).domains(List.of(Domain.OTHER)).build();

        when(skillRepository.findByNameAndUserId("Java", 10L)).thenReturn(Optional.of(existing));
        when(skillRepository.findByNameAndUserId("K8s", 10L)).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenReturn(created);
        when(experienceRepository.save(any(Experience.class))).thenAnswer(inv -> inv.getArgument(0));

        Experience saved = service.createExperience(request);

        assertThat(saved.getSkills()).extracting(Skill::getName).containsExactly("Java", "K8s");
        verify(skillRepository).save(any(Skill.class));
    }

    @Test
    void updateExperience_updatesFieldsAndSkillsWhenProvided() {
        Experience existing = Experience.builder().id(5L).userId(10L).title("Old").skills(List.of()).build();
        ExperienceRequest request = baseRequest();
        request.setTitle("Updated");
        request.setSkillNames(List.of("Docker"));
        Skill docker = Skill.builder().id(7L).name("Docker").userId(10L).build();

        when(experienceRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(skillRepository.findByNameAndUserId("Docker", 10L)).thenReturn(Optional.of(docker));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(inv -> inv.getArgument(0));

        Experience out = service.updateExperience(5L, request);

        assertThat(out.getTitle()).isEqualTo("Updated");
        assertThat(out.getSkills()).containsExactly(docker);
    }

    @Test
    void updateExperience_throwsWhenMissing() {
        when(experienceRepository.findById(99L)).thenReturn(Optional.empty());
        ExperienceRequest request = baseRequest();
        assertThatThrownBy(() -> service.updateExperience(99L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void readAndDeleteOperations_delegateToRepository() {
        Experience e = Experience.builder().id(1L).build();
        when(experienceRepository.findAll()).thenReturn(List.of(e));
        when(experienceRepository.findById(1L)).thenReturn(Optional.of(e));
        when(experienceRepository.findByUserIdOrderByStartDateDesc(10L)).thenReturn(List.of(e));
        when(experienceRepository.countExperiencesGroupedByDomain())
            .thenReturn(List.of(new ExperienceDomainStatDto(Domain.WEB_DEVELOPMENT, 3L)));

        assertThat(service.getAllExperiences()).hasSize(1);
        assertThat(service.getExperienceById(1L)).contains(e);
        assertThat(service.getExperiencesByUserId(10L)).hasSize(1);
        assertThat(service.getExperienceStatsByDomain()).hasSize(1);

        service.deleteExperience(1L);
        verify(experienceRepository).deleteById(1L);
    }

    private ExperienceRequest baseRequest() {
        ExperienceRequest request = new ExperienceRequest();
        request.setUserId(10L);
        request.setTitle("Portfolio Work");
        request.setType(ExperienceType.JOB);
        request.setDomain(Domain.WEB_DEVELOPMENT);
        request.setDescription("desc");
        request.setStartDate(LocalDate.now().minusMonths(2));
        request.setEndDate(LocalDate.now().minusMonths(1));
        request.setCompanyOrClientName("Client");
        request.setKeyTasks(List.of("Task1"));
        return request;
    }
}
