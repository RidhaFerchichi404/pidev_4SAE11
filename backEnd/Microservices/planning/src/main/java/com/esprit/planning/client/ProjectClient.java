package com.esprit.planning.client;

import com.esprit.planning.dto.ProjectDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for the Project microservice (to resolve project owner / clientId).
 */
@FeignClient(name = "project", url = "${project.service.url:http://localhost:8084}", path = "/projects")
public interface ProjectClient {

    @GetMapping("/{id}")
    ProjectDto getProjectById(@PathVariable("id") Long id);

    @GetMapping("/list")
    java.util.List<ProjectDto> getProjects();
}
