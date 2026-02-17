package tn.esprit.project.Services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.project.Entities.ProjectApplication;
import tn.esprit.project.Repository.ProjectApplicationRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class IProjectApplicationServicelmp implements IProjectApplicationService{
    private ProjectApplicationRepository projectApplicationRepository;
    @Override
    public ProjectApplication addProjectApplication(ProjectApplication projectApplication) {
        return projectApplicationRepository.save(projectApplication);
    }
    @Override
    public ProjectApplication updateProjectApplication(ProjectApplication projectApplication) {
        return projectApplicationRepository.save(projectApplication);
    }

    @Override
    public void deleteProjectApplication(Long id) {
        projectApplicationRepository.deleteById(id);
    }
    @Override
    public ProjectApplication getProjectApplicationById(Long id) {
        return projectApplicationRepository.findById(id).orElse(null);
    }
    @Override
    public List<ProjectApplication> getApplicationsByProject(Long projectId) {
        return projectApplicationRepository.findByProjectId(projectId);
    }

    @Override
    public List<ProjectApplication> getApplicationsByFreelance(Long freelanceId) {
        return projectApplicationRepository.findByFreelanceId(freelanceId);
    }
}
