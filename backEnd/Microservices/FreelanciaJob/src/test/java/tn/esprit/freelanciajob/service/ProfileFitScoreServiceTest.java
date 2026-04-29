package tn.esprit.freelanciajob.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.freelanciajob.Client.ExperienceClient;
import tn.esprit.freelanciajob.Client.SkillClient;
import tn.esprit.freelanciajob.Dto.ExperienceDto;
import tn.esprit.freelanciajob.Dto.Skills;
import tn.esprit.freelanciajob.Dto.response.FitScoreResponse;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Repository.JobRepository;
import tn.esprit.freelanciajob.Service.ProfileFitScoreService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileFitScoreServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private SkillClient skillClient;
    @Mock
    private ExperienceClient experienceClient;

    private ProfileFitScoreService service() {
        return new ProfileFitScoreService(jobRepository, skillClient, experienceClient, new ObjectMapper());
    }

    @Test
    void computeFitScoreUsesFallbackWhenApiKeyMissing() {
        ProfileFitScoreService service = service();
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "apiModel", "openai/gpt-5.2");
        ReflectionTestUtils.setField(service, "apiUrl", "http://localhost");

        Job job = Job.builder()
            .id(1L)
            .clientId(1L)
            .clientType(ClientType.INDIVIDUAL)
            .title("Spring Boot API Developer")
            .description("Build REST APIs with Java and SQL")
            .category("Backend")
            .locationType(LocationType.REMOTE)
            .requiredSkillIds(List.of(1L, 2L))
            .build();

        Skills java = new Skills();
        java.setId(1L);
        java.setName("Java");
        Skills spring = new Skills();
        spring.setId(2L);
        spring.setName("Spring Boot");

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(skillClient.getSkillsByUserId(99L)).thenReturn(List.of(java));
        when(skillClient.getSkillsByIds(List.of(1L, 2L))).thenReturn(List.of(java, spring));
        when(experienceClient.getExperiencesByUserId(99L))
            .thenReturn(List.of(new ExperienceDto(1L, "Backend Dev", "JOB", "Backend", "desc", null, null, "Client", List.of())));

        FitScoreResponse result = service.computeFitScore(1L, 99L);

        assertThat(result.getScore()).isBetween(0, 100);
        assertThat(result.getTier()).isNotBlank();
        assertThat(result.getMatchedSkills()).isNotEmpty();
        assertThat(result.getRecommendations()).isNotEmpty();
    }

    @Test
    void computeFitScoreThrowsNotFoundWhenJobMissing() {
        ProfileFitScoreService service = service();
        when(jobRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.computeFitScore(404L, 55L))
            .isInstanceOf(ResponseStatusException.class);
    }
}
