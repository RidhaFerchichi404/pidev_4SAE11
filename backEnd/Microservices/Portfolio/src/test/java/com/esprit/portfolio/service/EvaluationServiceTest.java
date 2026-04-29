package com.esprit.portfolio.service;

import com.esprit.portfolio.dto.AnswerSubmission;
import com.esprit.portfolio.dto.TestSubmission;
import com.esprit.portfolio.entity.Evaluation;
import com.esprit.portfolio.entity.EvaluationTest;
import com.esprit.portfolio.entity.Question;
import com.esprit.portfolio.entity.Skill;
import com.esprit.portfolio.repository.EvaluationRepository;
import com.esprit.portfolio.repository.EvaluationTestRepository;
import com.esprit.portfolio.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private EvaluationTestRepository evaluationTestRepository;

    @InjectMocks
    private EvaluationService service;

    @Test
    void create_savesWhenSkillBelongsToFreelancer() {
        Skill skill = Skill.builder().id(3L).userId(11L).name("Java").build();
        Evaluation input = Evaluation.builder().freelancerId(11L).score(0.0).passed(false).build();

        when(skillRepository.findById(3L)).thenReturn(Optional.of(skill));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        Evaluation saved = service.create(input, 3L);

        assertThat(saved.getSkill()).isEqualTo(skill);
    }

    @Test
    void create_throwsWhenSkillBelongsToAnotherFreelancer() {
        Skill skill = Skill.builder().id(3L).userId(99L).build();
        Evaluation input = Evaluation.builder().freelancerId(11L).build();
        when(skillRepository.findById(3L)).thenReturn(Optional.of(skill));

        assertThatThrownBy(() -> service.create(input, 3L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("does not belong");
    }

    @Test
    void submitEvaluation_computesScoreAndPassStatus() {
        Skill skill = Skill.builder().id(7L).name("Spring").build();
        EvaluationTest test = EvaluationTest.builder()
            .id(5L)
            .skill(skill)
            .passingScore(2.0)
            .questions(List.of(
                new Question("Q1", "A,B", "A", 2),
                new Question("Q2", "X,Y", "Y", 1)
            ))
            .build();

        TestSubmission submission = new TestSubmission();
        submission.setTestId(5L);
        submission.setFreelancerId(22L);
        AnswerSubmission a1 = new AnswerSubmission();
        a1.setQuestionIndex(0);
        a1.setSelectedOption("A");
        AnswerSubmission a2 = new AnswerSubmission();
        a2.setQuestionIndex(1);
        a2.setSelectedOption("X");
        submission.setAnswers(List.of(a1, a2));

        when(evaluationTestRepository.findById(5L)).thenReturn(Optional.of(test));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        Evaluation out = service.submitEvaluation(submission);

        assertThat(out.getScore()).isEqualTo(2.0);
        assertThat(out.getPassed()).isTrue();
        assertThat(out.getTestResult()).contains("1/2");
    }

    @Test
    void submitEvaluation_ignoresOutOfRangeQuestionIndexes() {
        Skill skill = Skill.builder().id(7L).name("Spring").build();
        EvaluationTest test = EvaluationTest.builder()
            .id(6L)
            .skill(skill)
            .passingScore(1.0)
            .questions(List.of(new Question("Q1", "A,B", "A", 1)))
            .build();

        TestSubmission submission = new TestSubmission();
        submission.setTestId(6L);
        submission.setFreelancerId(22L);
        AnswerSubmission invalid = new AnswerSubmission();
        invalid.setQuestionIndex(10);
        invalid.setSelectedOption("A");
        submission.setAnswers(List.of(invalid));

        when(evaluationTestRepository.findById(6L)).thenReturn(Optional.of(test));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> inv.getArgument(0));

        Evaluation out = service.submitEvaluation(submission);
        assertThat(out.getScore()).isEqualTo(0.0);
        assertThat(out.getPassed()).isFalse();
    }

    @Test
    void findAndDeleteOperations_coverBranches() {
        Evaluation e = Evaluation.builder().id(1L).build();
        when(evaluationRepository.findAll()).thenReturn(List.of(e));
        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(e));
        when(evaluationRepository.findByFreelancerId(2L)).thenReturn(List.of(e));
        when(evaluationRepository.findByFreelancerIdAndSkillId(2L, 3L)).thenReturn(Optional.of(e));
        when(evaluationRepository.existsById(1L)).thenReturn(true);

        assertThat(service.findAll()).hasSize(1);
        assertThat(service.findById(1L)).isEqualTo(e);
        assertThat(service.findByFreelancerId(2L)).hasSize(1);
        assertThat(service.findByFreelancerIdAndSkillId(2L, 3L)).isEqualTo(e);

        service.delete(1L);
        verify(evaluationRepository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenMissing() {
        when(evaluationRepository.existsById(111L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(111L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }
}
