package com.example.review.service;

import com.example.review.entity.Review;
import com.example.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    
    private final ReviewRepository reviewRepository;
    
    @Override
    public Review createReview(Review review) {
        return reviewRepository.save(review);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByReviewerId(Long reviewerId) {
        return reviewRepository.findByReviewerId(reviewerId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByRevieweeId(Long revieweeId) {
        return reviewRepository.findByRevieweeId(revieweeId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByProjectId(Long projectId) {
        return reviewRepository.findByProjectId(projectId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByRevieweeAndProject(Long revieweeId, Long projectId) {
        return reviewRepository.findByRevieweeIdAndProjectId(revieweeId, projectId);
    }
    
    @Override
    public Review updateReview(Long id, Review review) {
        return reviewRepository.findById(id)
                .map(existingReview -> {
                    existingReview.setReviewerId(review.getReviewerId());
                    existingReview.setRevieweeId(review.getRevieweeId());
                    existingReview.setProjectId(review.getProjectId());
                    existingReview.setRating(review.getRating());
                    existingReview.setComment(review.getComment());
                    return reviewRepository.save(existingReview);
                })
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));
    }
    
    @Override
    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }
}
