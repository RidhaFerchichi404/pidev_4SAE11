package com.esprit.aimodel.advice;

import com.esprit.aimodel.dto.AiErrorEnvelope;
import com.esprit.aimodel.exception.AiUpstreamException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;

import java.lang.reflect.Method;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class AiExceptionHandlerTest {

    @Test
    void handleBadJson_returnsBadRequestEnvelope() {
        AiExceptionHandler handler = new AiExceptionHandler();

        ResponseEntity<AiErrorEnvelope> response =
                handler.handleBadJson(new HttpMessageNotReadableException("bad json", new DummyInputMessage()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().message()).isEqualTo("Invalid JSON body");
    }

    @Test
    void handleUpstream_usesExceptionStatusAndMessage() {
        AiExceptionHandler handler = new AiExceptionHandler();
        AiUpstreamException ex = new AiUpstreamException(HttpStatus.SERVICE_UNAVAILABLE, "provider down");

        ResponseEntity<AiErrorEnvelope> response = handler.handleUpstream(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().error().message()).isEqualTo("provider down");
    }

    @Test
    void handleValidation_usesFirstFieldErrorMessage() throws Exception {
        AiExceptionHandler handler = new AiExceptionHandler();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new DummyBody(), "body");
        binding.addError(new FieldError("body", "prompt", "", false, null, null, "Prompt is required"));
        Method m = DummyBody.class.getDeclaredMethod("setPrompt", String.class);
        MethodParameter parameter = new MethodParameter(m, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, binding);

        ResponseEntity<AiErrorEnvelope> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().message()).isEqualTo("Prompt is required");
    }

    static class DummyBody {
        public void setPrompt(String prompt) {
            // no-op
        }
    }

    static class DummyInputMessage implements HttpInputMessage {
        @Override
        public InputStream getBody() {
            return InputStream.nullInputStream();
        }

        @Override
        public HttpHeaders getHeaders() {
            return HttpHeaders.EMPTY;
        }
    }
}
