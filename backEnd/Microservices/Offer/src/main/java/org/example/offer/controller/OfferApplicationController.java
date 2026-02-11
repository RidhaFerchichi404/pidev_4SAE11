package org.example.offer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.offer.dto.request.OfferApplicationRequest;
import org.example.offer.dto.response.OfferApplicationResponse;
import org.example.offer.service.OfferApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfferApplicationController {

    private final OfferApplicationService applicationService;

    /**
     * POST /api/applications - Postuler à une offre
     */
    @PostMapping
    public ResponseEntity<OfferApplicationResponse> applyToOffer(
            @Valid @RequestBody OfferApplicationRequest request) {
        OfferApplicationResponse response = applicationService.applyToOffer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/applications/offer/{offerId} - Récupérer les candidatures pour une offre
     */
    @GetMapping("/offer/{offerId}")
    public ResponseEntity<List<OfferApplicationResponse>> getApplicationsByOffer(
            @PathVariable Long offerId) {
        List<OfferApplicationResponse> applications =
                applicationService.getApplicationsByOfferId(offerId);
        return ResponseEntity.ok(applications);
    }

    /**
     * GET /api/applications/client/{clientId} - Récupérer les candidatures d'un client
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<OfferApplicationResponse>> getApplicationsByClient(
            @PathVariable Long clientId) {
        List<OfferApplicationResponse> applications =
                applicationService.getApplicationsByClientId(clientId);
        return ResponseEntity.ok(applications);
    }

    /**
     * GET /api/applications/pending - Récupérer toutes les candidatures en attente
     */
    @GetMapping("/pending")
    public ResponseEntity<List<OfferApplicationResponse>> getPendingApplications() {
        List<OfferApplicationResponse> applications =
                applicationService.getPendingApplications();
        return ResponseEntity.ok(applications);
    }

    /**
     * PATCH /api/applications/{id}/accept - Accepter une candidature
     */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<OfferApplicationResponse> acceptApplication(@PathVariable Long id) {
        OfferApplicationResponse response = applicationService.acceptApplication(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/applications/{id}/reject - Rejeter une candidature
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<OfferApplicationResponse> rejectApplication(@PathVariable Long id) {
        OfferApplicationResponse response = applicationService.rejectApplication(id);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/applications/{id} - Supprimer une candidature
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}