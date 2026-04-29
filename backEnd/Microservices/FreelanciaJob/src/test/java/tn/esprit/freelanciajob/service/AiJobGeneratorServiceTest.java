package tn.esprit.freelanciajob.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.freelanciajob.Dto.response.GeneratedJobDraft;
import tn.esprit.freelanciajob.Service.AiJobGeneratorService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AiJobGeneratorServiceTest {

    @Mock
    private Environment environment;

    @Test
    void generateJobDraftUsesFallbackWhenApiKeyMissing() {
        AiJobGeneratorService service = new AiJobGeneratorService(new ObjectMapper(), environment);
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "apiModel", "openai/gpt-5.2");
        ReflectionTestUtils.setField(service, "apiUrl", "http://localhost");

        GeneratedJobDraft draft = service.generateJobDraft(
            "Need a remote angular + spring boot developer to build APIs and dashboard"
        );

        assertThat(draft).isNotNull();
        assertThat(draft.getTitle()).containsIgnoringCase("Need a remote");
        assertThat(draft.getCategory()).isIn("Backend", "Web Development", "UI/UX Design");
        assertThat(draft.getLocationType()).isEqualTo("REMOTE");
        assertThat(draft.getCurrency()).isEqualTo("USD");
        assertThat(draft.getRequiredSkills()).isNotEmpty();
    }

    @Test
    void generateJobDraftFallbackInfersMobileAndOnsiteSignals() {
        AiJobGeneratorService service = new AiJobGeneratorService(new ObjectMapper(), environment);
        ReflectionTestUtils.setField(service, "apiKey", null);
        ReflectionTestUtils.setField(service, "apiModel", "openai/gpt-5.2");
        ReflectionTestUtils.setField(service, "apiUrl", "http://localhost");

        GeneratedJobDraft draft = service.generateJobDraft("Onsite flutter mobile app for ios and android");

        assertThat(draft.getCategory()).isEqualTo("Mobile Development");
        assertThat(draft.getLocationType()).isEqualTo("ONSITE");
        assertThat(draft.getEstimatedDurationWeeks()).isEqualTo(4);
        assertThat(draft.getBudgetMin()).isNotNull();
        assertThat(draft.getBudgetMax()).isNotNull();
    }
}
