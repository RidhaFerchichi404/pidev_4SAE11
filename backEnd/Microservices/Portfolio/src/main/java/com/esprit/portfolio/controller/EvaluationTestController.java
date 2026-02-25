package com.esprit.portfolio.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.esprit.portfolio.entity.EvaluationTest;
import com.esprit.portfolio.service.EvaluationTestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/evaluation-tests")
@RequiredArgsConstructor
public class EvaluationTestController {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationTestController.class);
    private final EvaluationTestService evaluationTestService;

    @GetMapping
    public ResponseEntity<List<EvaluationTest>> getAllTests() {
        return ResponseEntity.ok(evaluationTestService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluationTest> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationTestService.findById(id));
    }

    @GetMapping("/skill/{skillId}")
    public ResponseEntity<List<EvaluationTest>> getTestsBySkill(@PathVariable Long skillId) {
        return ResponseEntity.ok(evaluationTestService.findBySkillId(skillId));
    }

    @PostMapping("/skill/{skillId}")
    public ResponseEntity<EvaluationTest> createTest(@RequestBody EvaluationTest test, @PathVariable Long skillId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationTestService.create(test, skillId));
    }

    @PostMapping("/generate/{skillId}")
    public ResponseEntity<EvaluationTest> generateTest(@PathVariable Long skillId) {
        logger.info("Received request to generate test for skill ID: {}", skillId);
        try {
            EvaluationTest test = evaluationTestService.generateTestForSkill(skillId);
            logger.info("Successfully generated test with ID: {} for skill ID: {}", test.getId(), skillId);
            return ResponseEntity.status(HttpStatus.CREATED).body(test);
        } catch (Exception e) {
            logger.error("Error generating test for skill ID: {}", skillId, e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvaluationTest> updateTest(@PathVariable Long id, @RequestBody EvaluationTest test) {
        return ResponseEntity.ok(evaluationTestService.update(id, test));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long id) {
        evaluationTestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
