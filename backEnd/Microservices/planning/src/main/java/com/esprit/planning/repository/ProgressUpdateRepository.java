package com.esprit.planning.repository;

import com.esprit.planning.entity.ProgressUpdate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressUpdateRepository extends JpaRepository<ProgressUpdate, Long>, JpaSpecificationExecutor<ProgressUpdate> {

    List<ProgressUpdate> findByProjectId(Long projectId);

    List<ProgressUpdate> findByProjectIdAndCreatedAtBetween(Long projectId, LocalDateTime from, LocalDateTime to);

    List<ProgressUpdate> findByContractId(Long contractId);

    List<ProgressUpdate> findByFreelancerId(Long freelancerId);

    /** For stalled projects: projectId and its latest update time. */
    @Query("SELECT p.projectId, MAX(p.updatedAt) FROM ProgressUpdate p GROUP BY p.projectId")
    List<Object[]> findProjectIdAndMaxUpdatedAt();

    Optional<ProgressUpdate> findByProjectIdAndUpdatedAt(Long projectId, LocalDateTime updatedAt);

    /** Top freelancers by update count (order by count desc, limit via Pageable). */
    @Query("SELECT p.freelancerId, COUNT(p) FROM ProgressUpdate p GROUP BY p.freelancerId ORDER BY COUNT(p) DESC")
    List<Object[]> findFreelancerIdAndUpdateCountOrderByCountDesc(Pageable pageable);

    /** Most active projects by update count, optional date range on createdAt. */
    @Query("SELECT p.projectId, COUNT(p) FROM ProgressUpdate p WHERE (:from IS NULL OR p.createdAt >= :from) AND (:to IS NULL OR p.createdAt <= :to) GROUP BY p.projectId ORDER BY COUNT(p) DESC")
    List<Object[]> findProjectIdAndUpdateCountOrderByCountDescBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
