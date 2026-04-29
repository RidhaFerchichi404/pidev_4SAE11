package tn.esprit.freelanciajob;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpStatus;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.freelanciajob.Client.SkillClientFallback;
import tn.esprit.freelanciajob.Client.UserClientFallback;
import tn.esprit.freelanciajob.Controller.ApiExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;

class FreelanciaJobCoverageHelpersTest {

    @Test
    void mainRunsSpringApplication() {
        try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
            FreelanciaJobApplication.main(new String[0]);
            mocked.verify(() -> SpringApplication.run(FreelanciaJobApplication.class, new String[0]));
        }
    }

    @Test
    void fallbacksReturnSafeDefaults() {
        UserClientFallback userFallback = new UserClientFallback();
        assertThat(userFallback.getUserById(1L)).isNull();
        assertThat(userFallback.getUsersByRole("ADMIN")).isEmpty();

        SkillClientFallback skillFallback = new SkillClientFallback();
        assertThat(skillFallback.getSkillsByIds(java.util.List.of(1L, 2L))).isEmpty();
        assertThat(skillFallback.getSkillsByUserId(7L)).isEmpty();
    }

    @Test
    void apiExceptionHandlerBuildsErrorBodies() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "title", "required"));
        org.springframework.web.bind.MethodArgumentNotValidException ex =
                new org.springframework.web.bind.MethodArgumentNotValidException(Mockito.mock(MethodParameter.class), binding);

        var validation = handler.handleValidation(ex);
        assertThat(validation.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var status = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.CONFLICT, "duplicate"));
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var runtime = handler.handleRuntime(new RuntimeException("boom"));
        assertThat(runtime.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
