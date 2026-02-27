package tn.esprit.project.Services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.project.Entities.Enums.ApplicationStatus;
import tn.esprit.project.Entities.Enums.ProjectStatus;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Repository.ProjectRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class IProjectServicelmp implements IProjectService{

    private static class ProjectScore {

        private Project project;
        private int score;

        public ProjectScore(Project project, int score) {
            this.project = project;
            this.score = score;
        }

        public Project getProject() {
            return project;
        }

        public int getScore() {
            return score;
        }
    }


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

    @Override
    public List<Project> getProjectsByClientId(Long clientId) {
        return projectRepository.findByClientId(clientId);
    }

    @Override
    public List<Project> getRecommendedProjects(Long freelancerId) {

        // 🔹 Static skills for now
        List<String> freelancerSkills = List.of(
                "C++",
                "Go",
                "Java",
                "Spring",
                "Angular"
        );

        List<Project> openProjects =
                projectRepository.findByStatus(ProjectStatus.OPEN);

        return openProjects.stream()
                // 1️⃣ Keep only matching projects
                .map(project -> {
                    int score = calculateScore(project, freelancerSkills);
                    return new ProjectScore(project, score);
                })
                .filter(ps -> ps.getScore() > 0)   // 👈 IMPORTANT
                // 2️⃣ Sort by best match
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                // 3️⃣ Limit to 3 max
                .limit(3)
                .map(ProjectScore::getProject)
                .toList();
    }

    private int calculateScore(Project project, List<String> freelancerSkills) {

        if (project.getSkillsRequiered() == null || project.getSkillsRequiered().isBlank()) {
            return 0;
        }

        // Convert freelancer skills to lowercase once
        List<String> freelancerSkillsNormalized = freelancerSkills
                .stream()
                .map(String::toLowerCase)
                .toList();

        // Convert project skills to list
        List<String> projectSkills = Arrays.stream(
                        project.getSkillsRequiered().split(",")
                )
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();

        // Count matches
        return (int) projectSkills.stream()
                .filter(freelancerSkillsNormalized::contains)
                .count();
    }


    @Override
    public Map<String, Object> getProjectStatistics() {

        List<Project> projects = projectRepository.findAll();

        long total = projects.size();

        long open = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.OPEN)
                .count();

        long inProgress = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.IN_PROGRESS)
                .count();

        long completed = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED)
                .count();

        long cancelled = projects.stream()
                .filter(p -> p.getStatus() == ProjectStatus.CANCELLED)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProjects", total);
        stats.put("openProjects", open);
        stats.put("inProgressProjects", inProgress);
        stats.put("completedProjects", completed);
        stats.put("cancelledProjects", cancelled);

        return stats;
    }
}

