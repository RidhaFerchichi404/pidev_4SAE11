package com.esprit.task.service;

import com.esprit.task.entity.TaskComment;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.repository.TaskCommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskCommentServiceTest {

    @Mock
    private TaskCommentRepository taskCommentRepository;

    @InjectMocks
    private TaskCommentService taskCommentService;

    private static TaskComment comment(Long id, Long taskId) {
        TaskComment c = new TaskComment();
        c.setId(id);
        c.setTaskId(taskId);
        c.setUserId(10L);
        c.setMessage("Test");
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    @Test
    void findById_whenFound_returnsComment() {
        TaskComment c = comment(1L, 1L);
        when(taskCommentRepository.findById(1L)).thenReturn(Optional.of(c));

        TaskComment result = taskCommentService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("Test");
    }

    @Test
    void findById_whenNotFound_throws() {
        when(taskCommentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskCommentService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("TaskComment not found");
    }

    @Test
    void findByTaskId_returnsList() {
        TaskComment c = comment(1L, 1L);
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(c));

        List<TaskComment> result = taskCommentService.findByTaskId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTaskId()).isEqualTo(1L);
    }

    @Test
    void create_savesAndReturns() {
        TaskComment c = comment(null, 1L);
        c.setId(null);
        when(taskCommentRepository.save(any())).thenAnswer(inv -> {
            TaskComment saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TaskComment result = taskCommentService.create(c);

        assertThat(result).isNotNull();
        verify(taskCommentRepository).save(any());
    }

    @Test
    void deleteById_deletesComment() {
        TaskComment c = comment(1L, 1L);
        when(taskCommentRepository.findById(1L)).thenReturn(Optional.of(c));

        taskCommentService.deleteById(1L);

        verify(taskCommentRepository).delete(c);
    }

    @Test
    void findAll_returnsPaginated() {
        TaskComment c = comment(1L, 1L);
        Page<TaskComment> page = new PageImpl<>(List.of(c), PageRequest.of(0, 20), 1);
        when(taskCommentRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<TaskComment> result = taskCommentService.findAll(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMessage()).isEqualTo("Test");
    }

    @Test
    void update_modifiesMessageAndSaves() {
        TaskComment existing = comment(1L, 1L);
        TaskComment updated = comment(1L, 1L);
        updated.setMessage("Updated message");
        when(taskCommentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskCommentRepository.save(any())).thenReturn(updated);

        TaskComment result = taskCommentService.update(1L, updated);

        assertThat(result.getMessage()).isEqualTo("Updated message");
        verify(taskCommentRepository).save(any());
    }
}
