package com.esprit.portfolio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.esprit.portfolio.dto.SkillDomainStatDto;
import com.esprit.portfolio.dto.SkillSuccessStatDto;
import com.esprit.portfolio.dto.SkillUsageStatDto;
import com.esprit.portfolio.entity.Domain;
import com.esprit.portfolio.entity.Skill;
import com.esprit.portfolio.service.SkillService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    // ------------------------------------------------------------------ //
    //  Domain metadata — drives the checkbox list in the frontend
    // ------------------------------------------------------------------ //

    /**
     * GET /api/skills/domains
     * Returns all Domain enum values so the frontend can render checkboxes.
     */
    @GetMapping("/domains")
    public ResponseEntity<List<Domain>> getAllDomains() {
        return ResponseEntity.ok(skillService.getAllDomains());
    }

    // ------------------------------------------------------------------ //
    //  Admin statistics
    // ------------------------------------------------------------------ //

    /**
     * GET /api/skills/stats/by-domain
     * Admin: distinct skills count grouped by domain.
     */
    @GetMapping("/stats/by-domain")
    public ResponseEntity<List<SkillDomainStatDto>> getSkillStatsByDomain() {
        return ResponseEntity.ok(skillService.getSkillStatsByDomain());
    }

    /**
     * GET /api/skills/stats/usage
     * Admin: per skill name, how many freelancers have it + percentage.
     */
    @GetMapping("/stats/usage")
    public ResponseEntity<List<SkillUsageStatDto>> getSkillUsageStats() {
        return ResponseEntity.ok(skillService.getSkillUsageStats());
    }

    /**
     * GET /api/skills/stats/success
     * Admin: per skill, total test attempts and success rate (% passed).
     */
    @GetMapping("/stats/success")
    public ResponseEntity<List<SkillSuccessStatDto>> getSkillSuccessStats() {
        return ResponseEntity.ok(skillService.getSkillSuccessStats());
    }

    // ------------------------------------------------------------------ //
    //  Standard CRUD
    // ------------------------------------------------------------------ //

    @GetMapping
    public ResponseEntity<List<Skill>> getAllSkills() {
        return ResponseEntity.ok(skillService.findAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Skill>> getSkillsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(skillService.findAllByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Skill> getSkillById(@PathVariable Long id) {
        return ResponseEntity.ok(skillService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Skill> createSkill(@RequestBody Skill skill) {
        if (skill.getName() == null || skill.getName().isBlank()) {
            throw new IllegalArgumentException("Skill name is required");
        }
        if (skill.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        // domains list emptiness is caught by validateDomains() in the service
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.create(skill));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Skill> updateSkill(@PathVariable Long id, @RequestBody Skill skill) {
        return ResponseEntity.ok(skillService.update(id, skill));
    }

    /**
     * PATCH /api/skills/{id}/domains
     * Body: ["WEB_DEVELOPMENT", "VIDEO_MAKING"]
     * Freelancer operation: replace the checked domains for an existing skill.
     */
    @PatchMapping("/{id}/domains")
    public ResponseEntity<Skill> updateSkillDomains(
            @PathVariable Long id,
            @RequestBody List<Domain> domains) {
        return ResponseEntity.ok(skillService.updateSkillDomains(id, domains));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
