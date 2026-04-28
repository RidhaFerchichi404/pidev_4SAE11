package com.esprit.aimodel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private LlmGenerationService llmGenerationService;

    @InjectMocks
    private AiService aiService;

    @Test
    void generateResponse_overloadsDelegateToLlmService() {
        when(llmGenerationService.generate("hello", null)).thenReturn("world");
        when(llmGenerationService.generate("short", 120)).thenReturn("ok");

        assertThat(aiService.generateResponse("hello")).isEqualTo("world");
        assertThat(aiService.generateResponse("short", 120)).isEqualTo("ok");
    }

    @Test
    void generateTasksAndSubtasks_applyPromptPrefixAndNoiseStripping() {
        when(llmGenerationService.generate(anyString(), eq(null)))
                .thenReturn("```json\n[{\"title\":\"Task\"}]\n```");

        String tasks = aiService.generateTasks("Build feature X");
        String subtasks = aiService.generateSubtasks("Implement login");

        assertThat(tasks).contains("title");
        assertThat(subtasks).contains("title");
    }
}
