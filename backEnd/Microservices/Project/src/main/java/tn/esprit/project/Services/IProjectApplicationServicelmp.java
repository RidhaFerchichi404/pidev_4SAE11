package tn.esprit.project.Services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.project.Entities.Enums.ApplicationStatus;
import tn.esprit.project.Entities.ProjectApplication;
import tn.esprit.project.Repository.ProjectApplicationRepository;

import java.time.LocalDateTime;
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

        ProjectApplication existing = projectApplicationRepository
                .findById(projectApplication.getId())
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Update editable fields
        if (projectApplication.getProposedPrice() != null) {
            existing.setProposedPrice(projectApplication.getProposedPrice());
        }

        if (projectApplication.getProposedDuration() != null) {
            existing.setProposedDuration(projectApplication.getProposedDuration());
        }

        if (projectApplication.getCoverLetter() != null) {
            existing.setCoverLetter(projectApplication.getCoverLetter());
        }

        if (projectApplication.getStatus() != null) {
            existing.setStatus(projectApplication.getStatus());
        }

        return projectApplicationRepository.save(existing);
    }

    @Override
    public ProjectApplication updateStatus(Long id, ApplicationStatus status) {

        ProjectApplication existing = projectApplicationRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        existing.setStatus(status);
        existing.setRespondedAt(LocalDateTime.now());

        return projectApplicationRepository.save(existing);
    }

    @Override
    public void deleteProjectApplication(Long id) {
        projectApplicationRepository.deleteById(id);
    }

    @Override
    public List<ProjectApplication> getAllProjectApplications() {
        return projectApplicationRepository.findAll();
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
