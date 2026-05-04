package tn.esprit.project.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.project.Client.dto.ReviewFeignDto;

import java.util.List;

@FeignClient(
        name = "reviewFeign",
        url = "${project.integration.review-url:http://localhost:8085}",
        configuration = ProjectInterServiceFeignConfig.class)
public interface ReviewFeignClient {

    @GetMapping("/api/reviews/project/{projectId}")
    List<ReviewFeignDto> getReviewsByProject(@PathVariable("projectId") Long projectId);
}
