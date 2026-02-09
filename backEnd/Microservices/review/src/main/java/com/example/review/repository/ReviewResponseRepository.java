package com.example.review.repository;

import com.example.review.entity.ReviewResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewResponseRepository extends JpaRepository<ReviewResponse, Long> {
    
    List<ReviewResponse> findByReviewId(Long reviewId);
    
    List<ReviewResponse> findByRespondentId(Long respondentId);
}
