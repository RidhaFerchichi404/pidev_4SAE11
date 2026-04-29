package org.example.offer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.offer.config.GatewayOnlyFilter;
import org.example.offer.dto.request.ChatAssistantRequest;
import org.example.offer.exception.BadRequestException;
import org.example.offer.exception.GlobalExceptionHandler;
import org.example.offer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferAuxCoverageTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestTemplate restTemplate;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void chatAssistantService_handlesMissingAndSuccessfulConfig() {
        ChatAssistantService service = new ChatAssistantService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "apiUrl", "");
        ReflectionTestUtils.setField(service, "apiKey", "");
        assertThat(service.getReply(new ChatAssistantRequest("hello", List.of()))).isNull();

        ReflectionTestUtils.setField(service, "apiUrl", "http://ai.local/chat");
        ReflectionTestUtils.setField(service, "apiKey", "secret");
        ReflectionTestUtils.setField(service, "apiModel", "gpt");
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(), any(Class.class)))
                .thenReturn(ResponseEntity.ok("{\"choices\":[{\"message\":{\"content\":\"bonjour\"}}]}"));
        var response = service.getReply(new ChatAssistantRequest("bonjour", List.of()));
        assertThat(response).isNotNull();
        assertThat(response.getReply()).contains("bonjour");
    }

    @Test
    void emailService_respectsDisableAndSendsWhenEnabled() {
        EmailService service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "fromEmail", "no-reply@offer.tn");
        ReflectionTestUtils.setField(service, "mailEnabled", false);
        service.sendNewQuestionEmail("x@y.tn", "Ali", "Offer", "Question?", 1L);

        ReflectionTestUtils.setField(service, "mailEnabled", true);
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        service.sendNewQuestionEmail("x@y.tn", "Ali", "Offer", "Question?", 1L);
        verify(mailSender).send(message);

        doThrow(new RuntimeException("smtp")).when(mailSender).send(any(MimeMessage.class));
        service.sendNewQuestionEmail("x@y.tn", "Ali", "Offer", "Question?", 1L);
    }

    @Test
    void globalExceptionHandler_mapsKnownErrors() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        assertThat(handler.handleResourceNotFound(new ResourceNotFoundException("missing")).getStatusCode().value()).isEqualTo(404);
        assertThat(handler.handleBadRequest(new BadRequestException("bad")).getStatusCode().value()).isEqualTo(400);
        assertThat(handler.handleGeneric(new RuntimeException("boom")).getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void gatewayOnlyFilter_allowsWhitelistedAndBlocksDirectCalls() throws Exception {
        GatewayOnlyFilter filter = new GatewayOnlyFilter();

        MockHttpServletRequest health = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse healthRes = new MockHttpServletResponse();
        filter.doFilter(health, healthRes, new MockFilterChain());
        assertThat(healthRes.getStatus()).isNotEqualTo(403);

        MockHttpServletRequest blocked = new MockHttpServletRequest("GET", "/api/offers");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilter(blocked, blockedRes, new MockFilterChain());
        assertThat(blockedRes.getStatus()).isEqualTo(403);

        MockHttpServletRequest allowed = new MockHttpServletRequest("POST", "/api/offers");
        allowed.addHeader("X-Internal-Gateway", "true");
        MockHttpServletResponse allowedRes = new MockHttpServletResponse();
        filter.doFilter(allowed, allowedRes, new MockFilterChain());
        assertThat(allowedRes.getStatus()).isNotEqualTo(403);
    }
}
