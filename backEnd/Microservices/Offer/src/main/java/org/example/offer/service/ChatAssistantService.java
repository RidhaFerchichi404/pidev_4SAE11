package org.example.offer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.offer.dto.request.ChatAssistantRequest;
import org.example.offer.dto.response.ChatAssistantResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatAssistantService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.api.url:}")
    private String apiUrl;

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.model:gpt-4o-mini}")
    private String apiModel;

    private static final String SYSTEM_PROMPT = """
        Tu es l'assistant de la plateforme freelance Smart Freelance. Tu aides les clients qui parcourent les offres (Browse Offers).
        Tes réponses doivent être en français, courtes et utiles (2 à 5 phrases ou une liste à puces).
        Tu peux aider à :
        - Comprendre comment choisir une offre (lire la description, vérifier domaine/prix/durée, utiliser le Design Brief, poser des questions).
        - Expliquer ce qu'est un Design Brief (formulaire pour décrire le projet : identité visuelle, couleurs, type d'application, délais, budget ; à joindre à la candidature).
        - Donner des idées pour un projet (clarifier l'objectif, périmètre, public, budget/délai, utiliser les filtres).
        - Expliquer les prix (TND, fixed/hourly) et l'utilisation des filtres Min/Max Price.
        - Expliquer comment postuler (View & Apply, formulaire, joindre un Design Brief).
        Réponds de façon amicale et professionnelle. Si la question sort du cadre des offres et du parcours client, dis poliment que tu es là pour aider à parcourir et comprendre les offres.
        """;

    public ChatAssistantResponse getReply(ChatAssistantRequest request) {
        if (apiKey == null || apiKey.isBlank() || apiUrl == null || apiUrl.isBlank()) {
            return null;
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            for (ChatAssistantRequest.ChatMessageDto dto : request.getHistory()) {
                String role = "user".equalsIgnoreCase(dto.getRole()) ? "user" : "assistant";
                messages.add(Map.of("role", role, "content", dto.getText() != null ? dto.getText() : ""));
            }
        }
        messages.add(Map.of("role", "user", "content", request.getMessage()));

        Map<String, Object> body = Map.of(
                "model", apiModel,
                "messages", messages,
                "max_tokens", 1024,
                "temperature", 0.7
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            String reply = parseContent(response.getBody());
            return reply != null ? new ChatAssistantResponse(reply) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String parseContent(String json) {
        try {
            if (json == null) return null;
            JsonNode root = objectMapper.readTree(json);
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                return root.get("choices").get(0).path("message").path("content").asText(null);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
