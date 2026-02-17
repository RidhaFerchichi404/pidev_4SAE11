package tn.esprit.project.Services;

import tn.esprit.project.Entities.Project;

import java.util.List;

public interface IProjectService {
    Project addProject(Project project);
    Project updateProject(Project project);

    void deleteProject(Long id);

    Project getProjectById(Long id);

    List<Project> getAllProjects();
}
