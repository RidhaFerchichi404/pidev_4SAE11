package tn.esprit.project.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Services.IProjectService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final IProjectService projectService;

    @Value("${welcome.message}")
    private String welcomeMessage;

    @GetMapping("/welcome")
    public String welcome() {
        return welcomeMessage;
    }

    @PostMapping("/add")
    public Project addProject(@RequestBody Project project) {
        return projectService.addProject(project);
    }

    @PutMapping("/update")
    public Project updateProject(@RequestBody Project project) {
        return projectService.updateProject(project);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    @GetMapping("/list")
    public List<Project> getAllProjects() { return projectService.getAllProjects(); }

    @GetMapping("/client/{clientId}")
    public List<Project> getProjectsByClientId(@PathVariable Long clientId) {
        return projectService.getProjectsByClientId(clientId);
    }

    /** Single project by id (numeric only, so /client/14 is not matched here). */
    @GetMapping("/{id:\\d+}")
    public Project getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @GetMapping("/recommended")
    public List<Project> getRecommendedProjects(
            @RequestParam Long userId) {

        return projectService.getRecommendedProjects(userId);
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        return projectService.getProjectStatistics();
    }
}
