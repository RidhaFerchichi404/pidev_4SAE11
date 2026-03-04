package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.offer.client.UserFeignClient;
import org.example.offer.dto.request.AnswerQuestionRequest;
import org.example.offer.dto.request.OfferQuestionRequest;
import org.example.offer.dto.response.OfferQuestionResponse;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferQuestion;
import org.example.offer.entity.NotificationType;
import org.example.offer.exception.BadRequestException;
import org.example.offer.exception.ResourceNotFoundException;
import org.example.offer.repository.OfferQuestionRepository;
import org.example.offer.repository.OfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service Q&A : questions des clients sur une offre, réponses du freelancer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferQuestionService {

    private final OfferQuestionRepository questionRepository;
    private final OfferRepository offerRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserFeignClient userFeignClient;

    /**
     * Liste des questions-réponses pour une offre (ordre anti-chronologique).
     */
    @Transactional(readOnly = true)
    public List<OfferQuestionResponse> getQuestionsByOfferId(Long offerId) {
        List<OfferQuestion> list = questionRepository.findByOfferIdOrderByAskedAtDesc(offerId);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Un client pose une question sur une offre.
     */
    @Transactional
    public OfferQuestionResponse addQuestion(Long offerId, Long clientId, OfferQuestionRequest request) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + offerId));
        if (offer.getFreelancerId().equals(clientId)) {
            throw new BadRequestException("You cannot ask a question on your own offer.");
        }
        OfferQuestion q = new OfferQuestion();
        q.setOffer(offer);
        q.setClientId(clientId);
        q.setQuestionText(request.getQuestionText().trim());
        q = questionRepository.save(q);
        log.info("Question added for offer {} by client {}", offerId, clientId);

        // 1. Notification in-app pour le freelancer
        try {
            String title = "New question on your offer";
            String msg = "A client asked: \"" + (q.getQuestionText().length() > 80 ? q.getQuestionText().substring(0, 80) + "…" : q.getQuestionText()) + "\"";
            notificationService.createNotification(offer.getFreelancerId(), NotificationType.NEW_QUESTION, title, msg, offerId, q.getId());
        } catch (Exception e) {
            log.warn("Could not create notification for new question (offer={}, questionId={}): {}", offerId, q.getId(), e.getMessage());
        }

        // 2. Notifications externes au freelancer (asynchrone — ne bloque pas la reponse HTTP)
        try {
            log.info("[NOTIF] Fetching freelancer userId={} to send notifications", offer.getFreelancerId());
            Map<String, Object> freelancer = userFeignClient.getUserById(offer.getFreelancerId());
            log.info("[NOTIF] Freelancer data received: {}", freelancer);

            if (freelancer != null && !freelancer.isEmpty()) {
                String name = freelancer.containsKey("firstName")
                        ? (String) freelancer.get("firstName")
                        : (String) freelancer.getOrDefault("username", "Freelancer");

                // Email via Mailtrap
                String email = (String) freelancer.get("email");
                emailService.sendNewQuestionEmail(email, name, offer.getTitle(), q.getQuestionText(), offerId);
            } else {
                log.warn("[NOTIF] Freelancer data is empty for userId={} (User service down or fallback triggered)", offer.getFreelancerId());
            }
        } catch (Exception e) {
            log.error("[NOTIF] Error sending notifications (offer={}, questionId={}): {}", offerId, q.getId(), e.getMessage());
        }

        return toResponse(q);
    }

    /**
     * Le freelancer (propriétaire de l'offre) répond à une question.
     */
    @Transactional
    public OfferQuestionResponse answerQuestion(Long questionId, Long freelancerId, AnswerQuestionRequest request) {
        OfferQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));
        if (!q.getOffer().getFreelancerId().equals(freelancerId)) {
            throw new BadRequestException("Only the offer owner can answer this question.");
        }
        q.setAnswerText(request.getAnswerText().trim());
        q.setAnsweredAt(LocalDateTime.now());
        q = questionRepository.save(q);
        log.info("Question {} answered by freelancer {}", questionId, freelancerId);
        return toResponse(q);
    }

    private OfferQuestionResponse toResponse(OfferQuestion q) {
        OfferQuestionResponse r = new OfferQuestionResponse();
        r.setId(q.getId());
        r.setOfferId(q.getOffer().getId());
        r.setClientId(q.getClientId());
        r.setQuestionText(q.getQuestionText());
        r.setAnswerText(q.getAnswerText());
        r.setAskedAt(q.getAskedAt());
        r.setAnsweredAt(q.getAnsweredAt());
        r.setAnswered(q.isAnswered());
        return r;
    }
}
