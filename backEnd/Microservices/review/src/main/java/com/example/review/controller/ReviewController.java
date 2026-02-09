package com.example.review.controller;

import com.example.review.entity.Review;
import com.example.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Review", description = "Review management APIs")
public class ReviewController {
    
    private final ReviewService reviewService;
    
    @Operation(summary = "Create a new review", description = "Creates a new review for a project")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Review created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<Review> createReview(@RequestBody Review review) {
        Review createdReview = reviewService.createReview(review);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }
    
    @Operation(summary = "Get review by ID", description = "Retrieves a review by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review found"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Review> getReviewById(
            @Parameter(description = "ID of the review to retrieve") @PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(review -> new ResponseEntity<>(review, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @Operation(summary = "Get all reviews", description = "Retrieves all reviews")
    @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    @GetMapping
    public ResponseEntity<List<Review>> getAllReviews() {
        List<Review> reviews = reviewService.getAllReviews();
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }
    
    @GetMapping("/reviewer/{reviewerId}")
    public ResponseEntity<List<Review>> getReviewsByReviewerId(@PathVariable Long reviewerId) {
        List<Review> reviews = reviewService.getReviewsByReviewerId(reviewerId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }
    
    @GetMapping("/reviewee/{revieweeId}")
    public ResponseEntity<List<Review>> getReviewsByRevieweeId(@PathVariable Long revieweeId) {
        List<Review> reviews = reviewService.getReviewsByRevieweeId(revieweeId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }
    
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Review>> getReviewsByProjectId(@PathVariable Long projectId) {
        List<Review> reviews = reviewService.getReviewsByProjectId(projectId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }
    
    @GetMapping("/reviewee/{revieweeId}/project/{projectId}")
    public ResponseEntity<List<Review>> getReviewsByRevieweeAndProject(
            @PathVariable Long revieweeId, 
            @PathVariable Long projectId) {
        List<Review> reviews = reviewService.getReviewsByRevieweeAndProject(revieweeId, projectId);
        return new ResponseEntity<>(reviews, HttpStatus.OK);
    }
    
    @Operation(summary = "Update a review", description = "Updates an existing review by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review updated successfully"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Review> updateReview(
            @Parameter(description = "ID of the review to update") @PathVariable Long id, 
            @RequestBody Review review) {
        try {
            Review updatedReview = reviewService.updateReview(id, review);
            return new ResponseEntity<>(updatedReview, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @Operation(summary = "Delete a review", description = "Deletes a review by ID")
    @ApiResponse(responseCode = "204", description = "Review deleted successfully")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "ID of the review to delete") @PathVariable Long id) {
        reviewService.deleteReview(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
