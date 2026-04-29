package com.esprit.aimodel.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AImodelConfigTest {

    @Test
    void propertiesDefaultInvalidValuesToFiveSeconds() {
        AImodelProperties props = new AImodelProperties(0, -1);
        assertThat(props.connectTimeoutMs()).isEqualTo(5000);
        assertThat(props.readTimeoutMs()).isEqualTo(5000);
    }

    @Test
    void aiClientConfigBuildsFromBuilder() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        when(builder.build()).thenReturn(chatClient);

        AiClientConfig config = new AiClientConfig();
        ChatClient built = config.chatClient(builder);

        assertThat(built).isSameAs(chatClient);
    }

    @Test
    void ollamaRestClientConfigBuildsClientWithTrimmedBaseUrl() {
        OllamaRestClientConfig config = new OllamaRestClientConfig();
        RestClient client = config.ollamaTagsRestClient("http://localhost:11434/", new AImodelProperties(1234, 2345));
        assertThat(client).isNotNull();
    }
}
