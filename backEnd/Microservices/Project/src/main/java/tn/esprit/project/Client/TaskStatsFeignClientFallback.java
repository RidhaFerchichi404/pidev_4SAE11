package tn.esprit.project.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tn.esprit.project.Client.dto.TaskStatsFeignDto;

@Component
public class TaskStatsFeignClientFallback implements TaskStatsFeignClient {

    private static final Logger log = LoggerFactory.getLogger(TaskStatsFeignClientFallback.class);

    @Override
    public TaskStatsFeignDto getStatsByProject(Long projectId) {
        log.warn("Task stats Feign fallback for projectId={}", projectId);
        return null;
    }
}
