package com.esprit.portfolio.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esprit.portfolio.entity.EvaluationTest;
import com.esprit.portfolio.entity.Question;
import com.esprit.portfolio.entity.Skill;
import com.esprit.portfolio.repository.EvaluationTestRepository;
import com.esprit.portfolio.repository.SkillRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EvaluationTestService {

    private final EvaluationTestRepository evaluationTestRepository;
    private final SkillRepository skillRepository;
    private final AIService aiService;

    @Transactional(readOnly = true)
    public List<EvaluationTest> findAll() {
        return evaluationTestRepository.findAll();
    }

    @Transactional(readOnly = true)
    public EvaluationTest findById(Long id) {
        return evaluationTestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<EvaluationTest> findBySkillId(Long skillId) {
        return evaluationTestRepository.findBySkillId(skillId);
    }

    @Transactional
    public EvaluationTest create(EvaluationTest test, Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + skillId));
        test.setSkill(skill);
        return evaluationTestRepository.save(test);
    }

    @Transactional
    public EvaluationTest generateTestForSkill(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + skillId));

        List<Question> questions = aiService.generateQuestions(skill.getName());
        
        if (questions.isEmpty()) {
            throw new RuntimeException("Failed to generate questions for skill: " + skill.getName());
        }

        double totalPoints = questions.stream()
                .mapToDouble(q -> q.getPoints() != null ? q.getPoints() : 1.0)
                .sum();

        EvaluationTest test = EvaluationTest.builder()
                .skill(skill)
                .title("AI Generated Test for " + skill.getName())
                .questions(questions)
                .passingScore(totalPoints * 0.7) // 70% passing score
                .durationMinutes(30) // Default duration
                .build();

        return evaluationTestRepository.save(test);
    }

    @Transactional
    public EvaluationTest update(Long id, EvaluationTest testDetails) {
        EvaluationTest test = findById(id);
        test.setTitle(testDetails.getTitle());
        test.setPassingScore(testDetails.getPassingScore());
        test.setDurationMinutes(testDetails.getDurationMinutes());
        if (testDetails.getQuestions() != null) {
            test.setQuestions(testDetails.getQuestions());
        }
        return evaluationTestRepository.save(test);
    }

    @Transactional
    public void delete(Long id) {
        if (!evaluationTestRepository.existsById(id)) {
            throw new RuntimeException("Test not found with id: " + id);
        }
        evaluationTestRepository.deleteById(id);
    }
}
