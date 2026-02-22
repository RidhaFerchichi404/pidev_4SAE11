package tn.esprit.project.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.project.Entities.ProjectApplication;
import tn.esprit.project.Services.IProjectApplicationService;

import java.util.List;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ProjectApplicationController {

    private final IProjectApplicationService projectApplicationService;

    @PostMapping
    public ProjectApplication addApplication(@RequestBody ProjectApplication application) {
        return projectApplicationService.addProjectApplication(application);
    }

    @PutMapping
    public ProjectApplication updateApplication(@RequestBody ProjectApplication application) {
        return projectApplicationService.updateProjectApplication(application);
    }

    @DeleteMapping("/{id}")
    public void deleteApplication(@PathVariable Long id) {
        projectApplicationService.deleteProjectApplication(id);
    }

    @GetMapping("/{id}")
    public ProjectApplication getApplicationById(@PathVariable Long id) {
        return projectApplicationService.getProjectApplicationById(id);
    }

    @GetMapping("/project/{projectId}")
    public List<ProjectApplication> getApplicationsByProject(@PathVariable Long projectId) {
        return projectApplicationService.getApplicationsByProject(projectId);
    }

    @GetMapping("/all")
    public List<ProjectApplication> getApplications() {
        return projectApplicationService.getAllProjectApplications();
    }

    @GetMapping("/freelance/{freelanceId}")
    public List<ProjectApplication> getApplicationsByFreelance(@PathVariable Long freelanceId) {
        return projectApplicationService.getApplicationsByFreelance(freelanceId);
    }
}
