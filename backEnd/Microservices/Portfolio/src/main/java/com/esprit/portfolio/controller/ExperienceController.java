package com.esprit.portfolio.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.esprit.portfolio.dto.ExperienceRequest;
import com.esprit.portfolio.entity.Experience;
import com.esprit.portfolio.service.ExperienceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    @GetMapping
    public List<Experience> getAllExperiences() {
        return experienceService.getAllExperiences();
    }

    /** Declared before /{id} so "user" is not matched as numeric id. */
    @GetMapping("/user/{userId}")
    public List<Experience> getExperiencesByUserId(@PathVariable Long userId) {
        return experienceService.getExperiencesByUserId(userId);
    }

    /** {id} restricted to digits so /user/2 is not matched here. */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Experience> getExperienceById(@PathVariable Long id) {
        return experienceService.getExperienceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Experience createExperience(@RequestBody ExperienceRequest request) {
        return experienceService.createExperience(request);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Experience> updateExperience(@PathVariable Long id, @RequestBody ExperienceRequest request) {
        try {
            return ResponseEntity.ok(experienceService.updateExperience(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteExperience(@PathVariable Long id) {
        experienceService.deleteExperience(id);
        return ResponseEntity.noContent().build();
    }
}
