package com.esprit.aimodel.service;

import com.esprit.aimodel.exception.AiUpstreamException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmGenerationServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Test
    void generateReturnsContentForNormalPath() {
        when(chatClient.prompt().user("hello").call().content()).thenReturn("world");
        LlmGenerationService service = new LlmGenerationService(chatClient);

        String result = service.generate("hello", null);

        assertThat(result).isEqualTo("world");
    }

    @Test
    void generateUsesOptionsWhenMaxOutputTokensProvided() {
        when(chatClient.prompt().user(anyString()).options(any()).call().content()).thenReturn("short");
        LlmGenerationService service = new LlmGenerationService(chatClient);

        String result = service.generate("x", 120);

        assertThat(result).isEqualTo("short");
    }

    @Test
    void generateThrowsBadGatewayWhenProviderReturnsBlankContent() {
        when(chatClient.prompt().user("empty").call().content()).thenReturn(" ");
        LlmGenerationService service = new LlmGenerationService(chatClient);

        assertThatThrownBy(() -> service.generate("empty", null))
            .isInstanceOf(AiUpstreamException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void generateMapsConnectivityErrorsToServiceUnavailable() {
        RuntimeException wrapped = new RuntimeException(new ConnectException("refused"));
        when(chatClient.prompt().user("net").call().content()).thenThrow(wrapped);
        LlmGenerationService service = new LlmGenerationService(chatClient);

        assertThatThrownBy(() -> service.generate("net", null))
            .isInstanceOf(AiUpstreamException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void generateMapsUnexpectedErrorsToBadGateway() {
        when(chatClient.prompt().user("boom").call().content()).thenThrow(new RuntimeException("boom"));
        LlmGenerationService service = new LlmGenerationService(chatClient);

        assertThatThrownBy(() -> service.generate("boom", null))
            .isInstanceOf(AiUpstreamException.class)
            .hasMessageContaining("boom");
    }
}
