package tn.esprit.project.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Services.IProjectService;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final IProjectService projectService;

    @PostMapping
    public Project addProject(@RequestBody Project project) {
        return projectService.addProject(project);
    }

    @PutMapping
    public Project updateProject(@RequestBody Project project) {
        return projectService.updateProject(project);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }
}
