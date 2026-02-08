package org.example.offer.repository;

import org.example.offer.entity.OfferApplication;
import org.example.offer.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OfferApplicationRepository extends JpaRepository<OfferApplication, Long> {
    List<OfferApplication> findByOfferId(Long offerId);
    List<OfferApplication> findByClientId(Long clientId);
    List<OfferApplication> findByStatus(ApplicationStatus status);
}