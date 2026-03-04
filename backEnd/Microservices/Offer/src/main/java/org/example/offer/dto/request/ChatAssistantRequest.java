package org.example.offer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatAssistantRequest {

    @NotBlank(message = "Message is required")
    private String message;

    /** Optional conversation history: [{ "role": "user"|"assistant", "text": "..." }] */
    private List<ChatMessageDto> history;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageDto {
        private String role; // "user" or "assistant"
        private String text;
    }
}
