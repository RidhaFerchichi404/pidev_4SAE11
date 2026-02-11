package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import org.example.offer.dto.request.OfferApplicationRequest;
import org.example.offer.dto.response.OfferApplicationResponse;
import org.example.offer.entity.ApplicationStatus;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferApplication;
import org.example.offer.exception.BadRequestException;
import org.example.offer.exception.ResourceNotFoundException;
import org.example.offer.repository.OfferApplicationRepository;
import org.example.offer.repository.OfferRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OfferApplicationService {

    private final OfferApplicationRepository applicationRepository;
    private final OfferRepository offerRepository;
    private final ModelMapper modelMapper;

    /**
     * Postuler à une offre (Apply to Project)
     */
    public OfferApplicationResponse applyToOffer(OfferApplicationRequest request) {
        // Vérifier que l'offre existe
        Offer offer = offerRepository.findById(request.getOfferId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + request.getOfferId()));

        // Vérifier que l'offre est disponible
        if (!offer.getStatus().name().equals("AVAILABLE")) {
            throw new BadRequestException("This offer is not available for applications");
        }

        // Créer la candidature
        OfferApplication application = new OfferApplication();
        application.setOffer(offer);
        application.setClientId(request.getClientId());
        application.setMessage(request.getMessage());
        application.setProposedBudget(request.getProposedBudget());
        application.setStatus(ApplicationStatus.PENDING);

        OfferApplication savedApplication = applicationRepository.save(application);
        return mapToResponse(savedApplication);
    }

    /**
     * Récupérer toutes les candidatures pour une offre
     */
    public List<OfferApplicationResponse> getApplicationsByOfferId(Long offerId) {
        return applicationRepository.findByOfferId(offerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer toutes les candidatures d'un client
     */
    public List<OfferApplicationResponse> getApplicationsByClientId(Long clientId) {
        return applicationRepository.findByClientId(clientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les candidatures en attente (Pending)
     */
    public List<OfferApplicationResponse> getPendingApplications() {
        return applicationRepository.findByStatus(ApplicationStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Accepter une candidature
     */
    public OfferApplicationResponse acceptApplication(Long applicationId) {
        OfferApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        application.setStatus(ApplicationStatus.ACCEPTED);
        application.setRespondedAt(LocalDateTime.now());

        OfferApplication updatedApplication = applicationRepository.save(application);
        return mapToResponse(updatedApplication);
    }

    /**
     * Rejeter une candidature
     */
    public OfferApplicationResponse rejectApplication(Long applicationId) {
        OfferApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        application.setStatus(ApplicationStatus.REJECTED);
        application.setRespondedAt(LocalDateTime.now());

        OfferApplication updatedApplication = applicationRepository.save(application);
        return mapToResponse(updatedApplication);
    }

    /**
     * Supprimer une candidature
     */
    public void deleteApplication(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ResourceNotFoundException("Application not found with id: " + applicationId);
        }
        applicationRepository.deleteById(applicationId);
    }

    /**
     * Mapper OfferApplication -> OfferApplicationResponse
     */
    private OfferApplicationResponse mapToResponse(OfferApplication application) {
        OfferApplicationResponse response = modelMapper.map(application, OfferApplicationResponse.class);
        response.setOfferId(application.getOffer().getId());
        response.setOfferTitle(application.getOffer().getTitle());
        return response;
    }
}