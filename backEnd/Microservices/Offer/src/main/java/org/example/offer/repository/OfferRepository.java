package org.example.offer.repository;

import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    // Rechercher les offres par freelancer
    List<Offer> findByFreelancerId(Long freelancerId);

    // Rechercher les offres par statut
    List<Offer> findByStatus(OfferStatus status);

    // Rechercher les offres par domaine
    List<Offer> findByDomain(String domain);

    // Rechercher les offres par freelancer ET statut (MANQUANTE - À AJOUTER)
    List<Offer> findByFreelancerIdAndStatus(Long freelancerId, OfferStatus status);
}