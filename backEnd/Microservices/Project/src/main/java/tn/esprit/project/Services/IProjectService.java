package tn.esprit.project.Services;

import tn.esprit.project.Entities.Project;

public interface IProjectService {
    Project addProject(Project project);
    Project updateProject(Project project);

    void deleteProject(Project project);

    void deleteProject(Long id);

    Project getProjectById(Long id);
}
