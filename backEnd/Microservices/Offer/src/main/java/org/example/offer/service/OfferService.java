package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import org.example.offer.dto.request.OfferRequest;
import org.example.offer.dto.response.OfferResponse;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferStatus;
import org.example.offer.exception.ResourceNotFoundException;
import org.example.offer.repository.OfferRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OfferService {

    private final OfferRepository offerRepository;
    private final ModelMapper modelMapper;

    /**
     * Créer une nouvelle offre (Post New Project)
     */
    public OfferResponse createOffer(OfferRequest request) {
        Offer offer = modelMapper.map(request, Offer.class);
        offer.setStatus(OfferStatus.AVAILABLE);

        Offer savedOffer = offerRepository.save(offer);
        return mapToResponse(savedOffer);
    }

    /**
     * Récupérer toutes les offres d'un freelancer
     */
    public List<OfferResponse> getOffersByFreelancerId(Long freelancerId) {
        return offerRepository.findByFreelancerId(freelancerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer toutes les offres disponibles
     */
    public List<OfferResponse> getAvailableOffers() {
        return offerRepository.findByStatus(OfferStatus.AVAILABLE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer une offre par ID
     */
    public OfferResponse getOfferById(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));
        return mapToResponse(offer);
    }

    /**
     * Mettre à jour une offre
     */
    public OfferResponse updateOffer(Long id, OfferRequest request) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));

        offer.setTitle(request.getTitle());
        offer.setDomain(request.getDomain());
        offer.setDescription(request.getDescription());
        offer.setPrice(request.getPrice());
        offer.setDurationType(request.getDurationType());
        offer.setDeadline(request.getDeadline());
        offer.setCategory(request.getCategory());

        Offer updatedOffer = offerRepository.save(offer);
        return mapToResponse(updatedOffer);
    }

    /**
     * Changer le statut d'une offre
     */
    public OfferResponse changeOfferStatus(Long id, OfferStatus status) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));

        offer.setStatus(status);
        Offer updatedOffer = offerRepository.save(offer);
        return mapToResponse(updatedOffer);
    }

    /**
     * Supprimer une offre
     */
    public void deleteOffer(Long id) {
        if (!offerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Offer not found with id: " + id);
        }
        offerRepository.deleteById(id);
    }

    /**
     * Rechercher des offres par domaine
     */
    public List<OfferResponse> searchOffersByDomain(String domain) {
        return offerRepository.findByDomain(domain)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mapper Offer -> OfferResponse
     */
    private OfferResponse mapToResponse(Offer offer) {
        OfferResponse response = modelMapper.map(offer, OfferResponse.class);
        response.setApplicationsCount(offer.getApplications().size());
        return response;
    }
}