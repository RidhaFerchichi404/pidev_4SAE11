package tn.esprit.project.Services;

import tn.esprit.project.Entities.Project;
import tn.esprit.project.Entities.ProjectApplication;

import java.util.List;

public interface IProjectApplicationService {
    ProjectApplication addProjectApplication(ProjectApplication projectApplication);
    ProjectApplication updateProjectApplication(ProjectApplication projectApplication);
    void deleteProjectApplication(Long id);

    List<ProjectApplication> getAllProjectApplications();
    ProjectApplication getProjectApplicationById(Long id);
    List<ProjectApplication> getApplicationsByProject(Long projectId);
    List<ProjectApplication> getApplicationsByFreelance(Long freelanceId);
}
