package tn.esprit.project.Services;

import tn.esprit.project.Entities.Project;

import java.util.List;
import java.util.Map;

public interface IProjectService {
    Project addProject(Project project);
    Project updateProject(Project project);

    void deleteProject(Long id);

    Project getProjectById(Long id);

    List<Project> getAllProjects();

    List<Project> getProjectsByClientId(Long clientId);

    List<Project> getRecommendedProjects(Long freelancerId);

    Map<String, Object> getProjectStatistics();
}
