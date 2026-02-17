package tn.esprit.project.Mapper;

import tn.esprit.project.Dto.request.ProjectRequest;
import tn.esprit.project.Dto.response.ProjectResponse;
import tn.esprit.project.Entities.Project;

public class ProjectMapper {
    public static Project toEntity(ProjectRequest dto){
        Project p = new Project();
        p.setClientId(dto.getClientId());
        p.setTitle(dto.getTitle());
        p.setDescription(dto.getDescription());
        p.setBudget(dto.getBudget());
        p.setDeadline(dto.getDeadline());
        p.setCategory(dto.getCategory());
        p.setSkillsRequiered(dto.getSkillsRequiered());
        return p;
    }

    public static ProjectResponse toDTO(Project p){
        return new ProjectResponse(
                p.getId(),
                p.getClientId(),
                p.getTitle(),
                p.getDescription(),
                p.getBudget(),
                p.getDeadline(),
                p.getStatus().name(),
                p.getCategory()
        );
    }
}
