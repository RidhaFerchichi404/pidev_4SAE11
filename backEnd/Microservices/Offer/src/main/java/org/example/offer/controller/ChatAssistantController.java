package org.example.offer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.offer.dto.request.ChatAssistantRequest;
import org.example.offer.dto.response.ChatAssistantResponse;
import org.example.offer.service.ChatAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatAssistantController {

    private final ChatAssistantService chatAssistantService;

    @PostMapping("/assistant")
    public ResponseEntity<ChatAssistantResponse> chat(@Valid @RequestBody ChatAssistantRequest request) {
        ChatAssistantResponse response = chatAssistantService.getReply(request);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}
