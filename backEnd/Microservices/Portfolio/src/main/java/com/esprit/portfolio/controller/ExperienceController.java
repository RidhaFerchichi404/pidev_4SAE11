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

import com.esprit.portfolio.dto.ExperienceDomainStatDto;
import com.esprit.portfolio.dto.ExperienceRequest;
import com.esprit.portfolio.entity.Experience;
import com.esprit.portfolio.service.ExperienceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    /**
     * GET /api/experiences/stats/by-domain
     * Admin dashboard: total experiences grouped by domain.
     */
    @GetMapping("/stats/by-domain")
    public ResponseEntity<List<ExperienceDomainStatDto>> getExperienceStatsByDomain() {
        return ResponseEntity.ok(experienceService.getExperienceStatsByDomain());
    }

    @GetMapping
    public List<Experience> getAllExperiences() {
        return experienceService.getAllExperiences();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Experience> getExperienceById(@PathVariable Long id) {
        return experienceService.getExperienceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public List<Experience> getExperiencesByUserId(@PathVariable Long userId) {
        return experienceService.getExperiencesByUserId(userId);
    }

    @PostMapping
    public Experience createExperience(@RequestBody ExperienceRequest request) {
        return experienceService.createExperience(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Experience> updateExperience(@PathVariable Long id, @RequestBody ExperienceRequest request) {
        try {
            return ResponseEntity.ok(experienceService.updateExperience(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperience(@PathVariable Long id) {
        experienceService.deleteExperience(id);
        return ResponseEntity.noContent().build();
    }
}
