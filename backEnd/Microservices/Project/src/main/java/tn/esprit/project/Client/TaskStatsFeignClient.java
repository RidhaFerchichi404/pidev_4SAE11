package tn.esprit.project.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.project.Client.dto.TaskStatsFeignDto;

@FeignClient(
        name = "taskStats",
        url = "${project.integration.task-url:http://localhost:8091}",
        configuration = ProjectInterServiceFeignConfig.class,
        fallback = TaskStatsFeignClientFallback.class)
public interface TaskStatsFeignClient {

    @GetMapping("/api/tasks/stats/project/{projectId}")
    TaskStatsFeignDto getStatsByProject(@PathVariable("projectId") Long projectId);
}
