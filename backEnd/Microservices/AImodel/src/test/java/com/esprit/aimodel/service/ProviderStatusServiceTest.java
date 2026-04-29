package com.esprit.aimodel.service;

import com.esprit.aimodel.dto.AiLiveStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderStatusServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient ollamaTagsRestClient;

    @Test
    void liveStatus_marksModelReadyWhenListed() {
        when(ollamaTagsRestClient.get().uri("/api/tags").retrieve().body(String.class))
                .thenReturn("{\"models\":[{\"name\":\"gemma3:4b\"}]}");
        ProviderStatusService service =
                new ProviderStatusService(ollamaTagsRestClient, new ObjectMapper(), "gemma3:4b");

        AiLiveStatus status = service.liveStatus();

        assertThat(status.isOllamaReachable()).isTrue();
        assertThat(status.isModelReady()).isTrue();
    }

    @Test
    void liveStatus_marksProviderDownWhenTagsCallFails() {
        when(ollamaTagsRestClient.get().uri("/api/tags").retrieve().body(String.class))
                .thenThrow(new RuntimeException("network error"));
        ProviderStatusService service =
                new ProviderStatusService(ollamaTagsRestClient, new ObjectMapper(), "gemma3:4b");

        AiLiveStatus status = service.liveStatus();

        assertThat(status.isOllamaReachable()).isFalse();
        assertThat(status.isModelReady()).isFalse();
        assertThat(status.getService()).isEqualTo("aimodel");
    }
}
