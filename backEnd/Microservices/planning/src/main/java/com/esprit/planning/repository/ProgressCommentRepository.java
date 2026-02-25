package com.esprit.planning.repository;

import com.esprit.planning.entity.ProgressComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProgressCommentRepository extends JpaRepository<ProgressComment, Long> {

    List<ProgressComment> findByProgressUpdate_Id(Long progressUpdateId);

    long countByProgressUpdate_IdIn(Collection<Long> progressUpdateIds);

    /** Count comments on progress updates submitted by the given freelancer. */
    @Query("SELECT COUNT(c) FROM ProgressComment c WHERE c.progressUpdate.freelancerId = :freelancerId")
    long countByProgressUpdate_FreelancerId(@Param("freelancerId") Long freelancerId);
}
