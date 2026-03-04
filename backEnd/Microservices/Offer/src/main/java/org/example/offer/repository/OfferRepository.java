package org.example.offer.repository;

import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long>, JpaSpecificationExecutor<Offer> {

    // ========== Recherches de base ==========
    List<Offer> findByFreelancerId(Long freelancerId);

    Long countByFreelancerId(Long freelancerId);

    List<Offer> findByOfferStatus(OfferStatus status);

    Page<Offer> findByOfferStatus(OfferStatus status, Pageable pageable);

    List<Offer> findByDomain(String domain);

    Page<Offer> findByDomain(String domain, Pageable pageable);

    List<Offer> findByFreelancerIdAndOfferStatus(Long freelancerId, OfferStatus status);

    // ========== Recherches avancées ==========

    @Query("SELECT o FROM Offer o WHERE o.isActive = true AND o.offerStatus = org.example.offer.entity.OfferStatus.AVAILABLE ORDER BY o.createdAt DESC")
    Page<Offer> findActiveOffers(Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.isFeatured = true AND o.isActive = true ORDER BY o.publishedAt DESC")
    List<Offer> findFeaturedOffers();

    @Query("SELECT o FROM Offer o WHERE o.offerStatus = org.example.offer.entity.OfferStatus.AVAILABLE AND o.isActive = true AND o.rating >= :minRating ORDER BY o.rating DESC")
    Page<Offer> findTopRatedOffers(@Param("minRating") BigDecimal minRating, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.offerStatus = org.example.offer.entity.OfferStatus.AVAILABLE AND o.isActive = true AND o.domain = :domain ORDER BY o.publishedAt DESC")
    Page<Offer> findAvailableOffersByDomain(@Param("domain") String domain, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.price BETWEEN :minPrice AND :maxPrice AND o.offerStatus = org.example.offer.entity.OfferStatus.AVAILABLE AND o.isActive = true")
    Page<Offer> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.title LIKE %:keyword% OR o.description LIKE %:keyword% OR o.tags LIKE %:keyword%")
    Page<Offer> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.deadline >= :date AND o.offerStatus = org.example.offer.entity.OfferStatus.AVAILABLE")
    List<Offer> findOffersExpiringSoon(@Param("date") LocalDateTime date);

    @Query("SELECT o FROM Offer o WHERE o.deadline < :now AND o.offerStatus = org.example.offer.entity.OfferStatus.AVAILABLE")
    List<Offer> findExpiredOffers(@Param("now") LocalDateTime now);

    // ========== Statistiques ==========

    @Query("SELECT COUNT(o) FROM Offer o WHERE o.freelancerId = :freelancerId AND o.offerStatus = :status")
    Long countByFreelancerIdAndStatus(@Param("freelancerId") Long freelancerId, @Param("status") OfferStatus status);

    @Query("SELECT AVG(o.rating) FROM Offer o WHERE o.freelancerId = :freelancerId")
    BigDecimal calculateAverageRating(@Param("freelancerId") Long freelancerId);

    @Query("SELECT SUM(o.price) FROM Offer o WHERE o.freelancerId = :freelancerId AND o.offerStatus = org.example.offer.entity.OfferStatus.ACCEPTED")
    BigDecimal calculateTotalRevenue(@Param("freelancerId") Long freelancerId);

    @Query("SELECT o FROM Offer o WHERE o.freelancerId = :freelancerId ORDER BY o.viewsCount DESC")
    Page<Offer> findMostViewedByFreelancer(@Param("freelancerId") Long freelancerId, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Offer o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    Long countOffersCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Offer o WHERE o.freelancerId = :freelancerId AND o.createdAt >= :start AND o.createdAt <= :end")
    Long countByFreelancerIdAndCreatedAtBetween(@Param("freelancerId") Long freelancerId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.viewsCount), 0) FROM Offer o WHERE o.freelancerId = :freelancerId")
    Long sumViewsByFreelancerId(@Param("freelancerId") Long freelancerId);

    // ========== Recherches par projectStatusId (référence externe au microservice Project) ==========

    @Query("SELECT o FROM Offer o WHERE o.projectStatusId = :statusId AND o.isActive = true")
    Page<Offer> findActiveOffersByProjectStatusId(@Param("statusId") Long statusId, Pageable pageable);

    /** Pour Smart Matching : offres disponibles dont l'ID n'est pas dans la liste (ex. déjà candidaté). */
    @Query("SELECT o FROM Offer o WHERE o.offerStatus = org.example.offer.entity.OfferStatus.AVAILABLE AND o.isActive = true AND o.id NOT IN :excludeIds")
    List<Offer> findAvailableOffersExcludingIds(@Param("excludeIds") List<Long> excludeIds, Pageable pageable);

    /** Offres disponibles (pour fallback quand le client n'a pas d'historique). */
    @Query("SELECT o FROM Offer o WHERE o.offerStatus = org.example.offer.entity.OfferStatus.AVAILABLE AND o.isActive = true ORDER BY o.isFeatured DESC, o.viewsCount DESC, o.createdAt DESC")
    List<Offer> findAvailableOffersForRecommendation(Pageable pageable);
}