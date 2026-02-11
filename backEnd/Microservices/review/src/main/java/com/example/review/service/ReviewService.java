package com.example.review.service;

import com.example.review.entity.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewService {
    
    Review createReview(Review review);
    
    Optional<Review> getReviewById(Long id);
    
    List<Review> getAllReviews();
    
    List<Review> getReviewsByReviewerId(Long reviewerId);
    
    List<Review> getReviewsByRevieweeId(Long revieweeId);
    
    List<Review> getReviewsByProjectId(Long projectId);
    
    List<Review> getReviewsByRevieweeAndProject(Long revieweeId, Long projectId);
    
    Review updateReview(Long id, Review review);
    
    void deleteReview(Long id);
}
