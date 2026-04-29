package com.example.review.controller;

import com.example.review.dto.ReviewPageResponse;
import com.example.review.dto.ReviewStats;
import com.example.review.entity.Review;
import com.example.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController controller;

    @Test
    void welcomeReturnsConfiguredMessage() {
        ReflectionTestUtils.setField((Object) controller, "welcomeMessage", "hello review");
        assertThat(controller.welcome()).isEqualTo("hello review");
    }

    @Test
    void basicCrudAndListingEndpointsDelegateToService() {
        Review review = new Review();
        review.setId(1L);
        ReviewStats stats = new ReviewStats(1L, 5.0, Map.of(5, 1L));
        ReviewPageResponse page = new ReviewPageResponse(List.of(review), 1, 1, 10, 0, true, true);

        when(reviewService.createReview(review)).thenReturn(review);
        when(reviewService.getAllReviews()).thenReturn(List.of(review));
        when(reviewService.getReviewsByReviewerId(2L)).thenReturn(List.of(review));
        when(reviewService.getReviewsByRevieweeId(3L)).thenReturn(List.of(review));
        when(reviewService.getReviewsByProjectId(4L)).thenReturn(List.of(review));
        when(reviewService.getReviewsByRevieweeAndProject(3L, 4L)).thenReturn(List.of(review));
        when(reviewService.getPage(null, null, 0, 10)).thenReturn(page);
        when(reviewService.getPageByReviewerId(2L, null, null, 0, 10)).thenReturn(page);
        when(reviewService.getPageByRevieweeId(3L, null, null, 0, 10)).thenReturn(page);
        when(reviewService.getStats()).thenReturn(stats);
        when(reviewService.getStatsByReviewer(2L)).thenReturn(stats);
        when(reviewService.getStatsByReviewee(3L)).thenReturn(stats);
        when(reviewService.getStatsByReviewerAndReviewee(2L, 3L)).thenReturn(stats);
        when(reviewService.getReviewsByReviewerAndReviewee(2L, 3L)).thenReturn(List.of(review));
        when(reviewService.getReviewById(1L)).thenReturn(Optional.of(review));

        assertThat(controller.createReview(review).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var allReviews = controller.getAllReviews().getBody();
        var byReviewer = controller.getReviewsByReviewerId(2L).getBody();
        var byReviewee = controller.getReviewsByRevieweeId(3L).getBody();
        var byProject = controller.getReviewsByProjectId(4L).getBody();
        var byRevieweeProject = controller.getReviewsByRevieweeAndProject(3L, 4L).getBody();
        var pageAll = controller.getPage(null, null, 0, 10).getBody();
        var pageReviewer = controller.getPageByReviewerId(2L, null, null, 0, 10).getBody();
        var pageReviewee = controller.getPageByRevieweeId(3L, null, null, 0, 10).getBody();
        var statsAll = controller.getStats().getBody();
        var statsReviewer = controller.getStatsByReviewer(2L).getBody();
        var statsReviewee = controller.getStatsByReviewee(3L).getBody();
        var statsPair = controller.getStatsByReviewerAndReviewee(2L, 3L).getBody();
        var pairReviews = controller.getReviewsByPair(2L, 3L).getBody();

        assertThat(allReviews).isNotNull();
        assertThat(byReviewer).isNotNull();
        assertThat(byReviewee).isNotNull();
        assertThat(byProject).isNotNull();
        assertThat(byRevieweeProject).isNotNull();
        assertThat(pageAll).isNotNull();
        assertThat(pageReviewer).isNotNull();
        assertThat(pageReviewee).isNotNull();
        assertThat(statsAll).isNotNull();
        assertThat(statsReviewer).isNotNull();
        assertThat(statsReviewee).isNotNull();
        assertThat(statsPair).isNotNull();
        assertThat(pairReviews).isNotNull();

        assertThat(allReviews).hasSize(1);
        assertThat(byReviewer).hasSize(1);
        assertThat(byReviewee).hasSize(1);
        assertThat(byProject).hasSize(1);
        assertThat(byRevieweeProject).hasSize(1);
        assertThat(pageAll.getTotalElements()).isEqualTo(1L);
        assertThat(pageReviewer.getTotalElements()).isEqualTo(1L);
        assertThat(pageReviewee.getTotalElements()).isEqualTo(1L);
        assertThat(statsAll.getTotalCount()).isEqualTo(1L);
        assertThat(statsReviewer.getTotalCount()).isEqualTo(1L);
        assertThat(statsReviewee.getTotalCount()).isEqualTo(1L);
        assertThat(statsPair.getTotalCount()).isEqualTo(1L);
        assertThat(pairReviews).hasSize(1);
        assertThat(controller.getReviewById(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateAndGetByIdHandleNotFoundBranches() {
        Review review = new Review();
        when(reviewService.updateReview(9L, review)).thenThrow(new RuntimeException("missing"));
        when(reviewService.getReviewById(999L)).thenReturn(Optional.empty());

        assertThat(controller.updateReview(9L, review).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.getReviewById(999L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
