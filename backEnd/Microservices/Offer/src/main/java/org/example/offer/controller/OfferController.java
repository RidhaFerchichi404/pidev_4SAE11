package org.example.offer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.offer.dto.request.OfferRequest;
import org.example.offer.dto.response.OfferResponse;
import org.example.offer.entity.OfferStatus;
import org.example.offer.service.OfferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfferController {

    private final OfferService offerService;

    /**
     * POST /api/offers - Créer une nouvelle offre (Post New Project)
     */
    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(@Valid @RequestBody OfferRequest request) {
        OfferResponse response = offerService.createOffer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/offers - Récupérer toutes les offres disponibles
     */
    @GetMapping
    public ResponseEntity<List<OfferResponse>> getAllAvailableOffers() {
        List<OfferResponse> offers = offerService.getAvailableOffers();
        return ResponseEntity.ok(offers);
    }

    /**
     * GET /api/offers/{id} - Récupérer une offre par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<OfferResponse> getOfferById(@PathVariable Long id) {
        OfferResponse offer = offerService.getOfferById(id);
        return ResponseEntity.ok(offer);
    }

    /**
     * GET /api/offers/freelancer/{freelancerId} - Récupérer les offres d'un freelancer
     */
    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<OfferResponse>> getOffersByFreelancer(@PathVariable Long freelancerId) {
        List<OfferResponse> offers = offerService.getOffersByFreelancerId(freelancerId);
        return ResponseEntity.ok(offers);
    }

    /**
     * GET /api/offers/domain/{domain} - Rechercher par domaine
     */
    @GetMapping("/domain/{domain}")
    public ResponseEntity<List<OfferResponse>> searchByDomain(@PathVariable String domain) {
        List<OfferResponse> offers = offerService.searchOffersByDomain(domain);
        return ResponseEntity.ok(offers);
    }

    /**
     * PUT /api/offers/{id} - Mettre à jour une offre
     */
    @PutMapping("/{id}")
    public ResponseEntity<OfferResponse> updateOffer(
            @PathVariable Long id,
            @Valid @RequestBody OfferRequest request) {
        OfferResponse response = offerService.updateOffer(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/offers/{id}/status - Changer le statut d'une offre
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OfferResponse> changeOfferStatus(
            @PathVariable Long id,
            @RequestParam OfferStatus status) {
        OfferResponse response = offerService.changeOfferStatus(id, status);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/offers/{id} - Supprimer une offre
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable Long id) {
        offerService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }
}