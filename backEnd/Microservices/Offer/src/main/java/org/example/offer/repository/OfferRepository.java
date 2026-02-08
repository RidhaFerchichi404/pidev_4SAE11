package org.example.offer.repository;

import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    List<Offer> findByFreelancerId(Long freelancerId);
    List<Offer> findByStatus(OfferStatus status);
    List<Offer> findByDomain(String domain);
}