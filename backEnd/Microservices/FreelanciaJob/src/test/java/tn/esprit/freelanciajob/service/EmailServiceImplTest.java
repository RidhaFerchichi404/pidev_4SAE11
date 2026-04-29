package tn.esprit.freelanciajob.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import tn.esprit.freelanciajob.Service.EmailServiceImpl;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    void sendSimpleEmail_buildsAndSendsMessage() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@smartfreelance.tn");
        ReflectionTestUtils.setField(emailService, "fromName", "Freelancia Platform");

        emailService.sendSimpleEmail("dev@platform.tn", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).contains("no-reply@smartfreelance.tn");
        assertThat(captor.getValue().getTo()).containsExactly("dev@platform.tn");
    }

    @Test
    void sendHtmlEmail_rendersTemplateAndSendsMimeMessage() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@smartfreelance.tn");
        ReflectionTestUtils.setField(emailService, "fromName", "Freelancia Platform");
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(templateEngine.process(eq("welcome"), any())).thenReturn("<b>Hello</b>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("mail@platform.tn", "Welcome", "welcome", Map.of("name", "Ali"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendSimpleEmail_doesNotPropagateMailException() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@smartfreelance.tn");
        ReflectionTestUtils.setField(emailService, "fromName", "Freelancia Platform");
        doThrow(new org.springframework.mail.MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService.sendSimpleEmail("dev@platform.tn", "Subject", "Body"))
                .doesNotThrowAnyException();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
