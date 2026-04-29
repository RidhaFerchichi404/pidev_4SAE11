package com.example.review.controller;

import com.example.review.entity.ReviewResponse;
import com.example.review.service.ReviewResponseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewResponseControllerTest {

    @Mock
    private ReviewResponseService reviewResponseService;

    @InjectMocks
    private ReviewResponseController controller;

    @Test
    void createAndQueryEndpointsReturnExpectedStatuses() {
        ReviewResponse response = new ReviewResponse();
        response.setId(1L);

        when(reviewResponseService.createResponse(response)).thenReturn(response);
        when(reviewResponseService.getResponseById(1L)).thenReturn(Optional.of(response));
        when(reviewResponseService.getResponseById(404L)).thenReturn(Optional.empty());
        when(reviewResponseService.getAllResponses()).thenReturn(List.of(response));
        when(reviewResponseService.getResponsesByReviewId(10L)).thenReturn(List.of(response));
        when(reviewResponseService.getResponsesByRespondentId(20L)).thenReturn(List.of(response));

        assertThat(controller.createResponse(response).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getResponseById(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getResponseById(404L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.getAllResponses().getBody()).hasSize(1);
        assertThat(controller.getResponsesByReviewId(10L).getBody()).hasSize(1);
        assertThat(controller.getResponsesByRespondentId(20L).getBody()).hasSize(1);
    }

    @Test
    void updateEndpointCoversValidationAndExceptionBranches() {
        ReviewResponse updated = new ReviewResponse();
        updated.setId(7L);
        when(reviewResponseService.updateResponse(7L, "ok")).thenReturn(updated);
        when(reviewResponseService.updateResponse(8L, "bad")).thenThrow(new IllegalArgumentException("bad"));
        when(reviewResponseService.updateResponse(9L, "missing")).thenThrow(new RuntimeException("missing"));

        assertThat(controller.updateResponse(7L, Map.of("message", "ok")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.updateResponse(7L, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.updateResponse(8L, Map.of("message", "bad")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.updateResponse(9L, Map.of("message", "missing")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteEndpointReturnsNoContent() {
        assertThat(controller.deleteResponse(50L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
