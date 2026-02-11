package com.esprit.planning.service;

import com.esprit.planning.client.UserClient;
import com.esprit.planning.dto.UserDto;
import com.esprit.planning.entity.ProgressComment;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.repository.ProgressCommentRepository;
import com.esprit.planning.repository.ProgressUpdateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressCommentService {

    private final ProgressCommentRepository progressCommentRepository;
    private final ProgressUpdateRepository progressUpdateRepository;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public List<ProgressComment> findAll() {
        return progressCommentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ProgressComment findById(Long id) {
        return progressCommentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProgressComment not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProgressComment> findByProgressUpdateId(Long progressUpdateId) {
        return progressCommentRepository.findByProgressUpdate_Id(progressUpdateId);
    }

    @Transactional
    public ProgressComment create(Long progressUpdateId, Long userId, String message) {
        ProgressUpdate progressUpdate = progressUpdateRepository.findById(progressUpdateId)
                .orElseThrow(() -> new RuntimeException("ProgressUpdate not found with id: " + progressUpdateId));

        // Optionally validate that the user exists in the User microservice.
        // This will throw if the user-service returns an error (4xx/5xx).
        UserDto user = userClient.getUserById(userId);

        ProgressComment comment = ProgressComment.builder()
                .progressUpdate(progressUpdate)
                .userId(user.getId())
                .message(message)
                .build();
        return progressCommentRepository.save(comment);
    }

    @Transactional
    public ProgressComment update(Long id, String message) {
        ProgressComment existing = findById(id);
        existing.setMessage(message);
        return progressCommentRepository.save(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!progressCommentRepository.existsById(id)) {
            throw new RuntimeException("ProgressComment not found with id: " + id);
        }
        progressCommentRepository.deleteById(id);
    }
}