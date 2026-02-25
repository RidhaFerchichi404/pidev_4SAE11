package com.esprit.portfolio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.esprit.portfolio.dto.TestSubmission;
import com.esprit.portfolio.entity.Evaluation;
import com.esprit.portfolio.service.EvaluationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping
    public ResponseEntity<List<Evaluation>> getAllEvaluations() {
        return ResponseEntity.ok(evaluationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evaluation> getEvaluationById(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.findById(id));
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<Evaluation>> getEvaluationsByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(evaluationService.findByFreelancerId(freelancerId));
    }

    @GetMapping("/freelancer/{freelancerId}/skill/{skillId}")
    public ResponseEntity<Evaluation> getEvaluationByFreelancerAndSkill(@PathVariable Long freelancerId, @PathVariable Long skillId) {
        Evaluation eval = evaluationService.findByFreelancerIdAndSkillId(freelancerId, skillId);
        return eval != null ? ResponseEntity.ok(eval) : ResponseEntity.notFound().build();
    }

    @PostMapping("/skill/{skillId}")
    public ResponseEntity<Evaluation> createEvaluation(@RequestBody Evaluation evaluation, @PathVariable Long skillId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationService.create(evaluation, skillId));
    }

    @PostMapping("/submit")
    public ResponseEntity<Evaluation> submitEvaluation(@RequestBody TestSubmission submission) {
        return ResponseEntity.ok(evaluationService.submitEvaluation(submission));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvaluation(@PathVariable Long id) {
        evaluationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
