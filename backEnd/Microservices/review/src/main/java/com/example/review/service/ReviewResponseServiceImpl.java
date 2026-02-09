package com.example.review.service;

import com.example.review.entity.ReviewResponse;
import com.example.review.repository.ReviewResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewResponseServiceImpl implements ReviewResponseService {
    
    private final ReviewResponseRepository reviewResponseRepository;
    
    @Override
    public ReviewResponse createResponse(ReviewResponse reviewResponse) {
        return reviewResponseRepository.save(reviewResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewResponse> getResponseById(Long id) {
        return reviewResponseRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllResponses() {
        return reviewResponseRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getResponsesByReviewId(Long reviewId) {
        return reviewResponseRepository.findByReviewId(reviewId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getResponsesByRespondentId(Long respondentId) {
        return reviewResponseRepository.findByRespondentId(respondentId);
    }
    
    @Override
    public ReviewResponse updateResponse(Long id, ReviewResponse reviewResponse) {
        return reviewResponseRepository.findById(id)
                .map(existingResponse -> {
                    existingResponse.setRespondentId(reviewResponse.getRespondentId());
                    existingResponse.setMessage(reviewResponse.getMessage());
                    return reviewResponseRepository.save(existingResponse);
                })
                .orElseThrow(() -> new RuntimeException("ReviewResponse not found with id: " + id));
    }
    
    @Override
    public void deleteResponse(Long id) {
        reviewResponseRepository.deleteById(id);
    }
}
