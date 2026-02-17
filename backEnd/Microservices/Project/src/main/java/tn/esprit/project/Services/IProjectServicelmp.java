package tn.esprit.project.Services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Repository.ProjectRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class IProjectServicelmp implements IProjectService{
    private ProjectRepository projectRepository;
    @Override
    public Project addProject(Project project) {
        return projectRepository.save(project);
    }
    @Override
    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    @Override
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
    @Override
    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElse(null);
    }

    @Override
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

}
