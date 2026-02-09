package com.example.review.repository;

import com.example.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByReviewerId(Long reviewerId);
    
    List<Review> findByRevieweeId(Long revieweeId);
    
    List<Review> findByProjectId(Long projectId);
    
    List<Review> findByRevieweeIdAndProjectId(Long revieweeId, Long projectId);
}
