package com.example.review.controller;

import com.example.review.entity.ReviewResponse;
import com.example.review.service.ReviewResponseService;
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
@RequestMapping("/api/review-responses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Review Response", description = "Review response management APIs")
public class ReviewResponseController {
    
    private final ReviewResponseService reviewResponseService;
    
    @Operation(summary = "Create a new review response", description = "Creates a response to a review")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Response created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<ReviewResponse> createResponse(@RequestBody ReviewResponse reviewResponse) {
        ReviewResponse createdResponse = reviewResponseService.createResponse(reviewResponse);
        return new ResponseEntity<>(createdResponse, HttpStatus.CREATED);
    }
    
    @Operation(summary = "Get response by ID", description = "Retrieves a review response by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Response found"),
        @ApiResponse(responseCode = "404", description = "Response not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getResponseById(
            @Parameter(description = "ID of the response to retrieve") @PathVariable Long id) {
        return reviewResponseService.getResponseById(id)
                .map(response -> new ResponseEntity<>(response, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @Operation(summary = "Get all responses", description = "Retrieves all review responses")
    @ApiResponse(responseCode = "200", description = "Responses retrieved successfully")
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getAllResponses() {
        List<ReviewResponse> responses = reviewResponseService.getAllResponses();
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }
    
    @GetMapping("/review/{reviewId}")
    public ResponseEntity<List<ReviewResponse>> getResponsesByReviewId(@PathVariable Long reviewId) {
        List<ReviewResponse> responses = reviewResponseService.getResponsesByReviewId(reviewId);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }
    
    @GetMapping("/respondent/{respondentId}")
    public ResponseEntity<List<ReviewResponse>> getResponsesByRespondentId(@PathVariable Long respondentId) {
        List<ReviewResponse> responses = reviewResponseService.getResponsesByRespondentId(respondentId);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateResponse(@PathVariable Long id, @RequestBody ReviewResponse reviewResponse) {
        try {
            ReviewResponse updatedResponse = reviewResponseService.updateResponse(id, reviewResponse);
            return new ResponseEntity<>(updatedResponse, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResponse(@PathVariable Long id) {
        reviewResponseService.deleteResponse(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
