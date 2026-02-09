package com.example.review.service;

import com.example.review.entity.ReviewResponse;

import java.util.List;
import java.util.Optional;

public interface ReviewResponseService {
    
    ReviewResponse createResponse(ReviewResponse reviewResponse);
    
    Optional<ReviewResponse> getResponseById(Long id);
    
    List<ReviewResponse> getAllResponses();
    
    List<ReviewResponse> getResponsesByReviewId(Long reviewId);
    
    List<ReviewResponse> getResponsesByRespondentId(Long respondentId);
    
    ReviewResponse updateResponse(Long id, ReviewResponse reviewResponse);
    
    void deleteResponse(Long id);
}
