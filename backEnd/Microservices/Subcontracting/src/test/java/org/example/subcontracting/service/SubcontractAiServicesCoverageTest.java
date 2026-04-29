package org.example.subcontracting.service;

import org.example.subcontracting.client.PortfolioFeignClient;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.PortfolioSkillDto;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.response.SubcontractMatchCandidateDto;
import org.example.subcontracting.dto.response.SubcontractMatchResponse;
import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractCategory;
import org.example.subcontracting.entity.SubcontractStatus;
import org.example.subcontracting.repository.SubcontractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubcontractAiServicesCoverageTest {

    @Mock private UserFeignClient userFeignClient;
    @Mock private PortfolioFeignClient portfolioFeignClient;
    @Mock private SubcontractRepository subcontractRepository;
    @Mock private SubcontractDashboardService dashboardService;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private SubcontractAiMatchService aiMatchService;

    @Mock private SubcontractAiMatchService nestedAiMatchService;
    @InjectMocks
    private SubcontractAiDecisionAssistantService aiDecisionAssistantService;

    @Test
    void aiMatchService_heuristicAndClaudePathsReturnCandidates() {
        UserRemoteDto main = user(1L, "Main", "Owner", "CLIENT", true);
        UserRemoteDto freelancer = user(2L, "Sara", "Dev", "FREELANCER", true);
        when(userFeignClient.getAllUsers()).thenReturn(List.of(main, freelancer));
        when(portfolioFeignClient.getSkillsByUserId(2L)).thenReturn(List.of(skill("Java"), skill("Spring Boot")));
        when(portfolioFeignClient.getExperiencesByUserId(2L)).thenReturn(List.of());
        when(dashboardService.computeScore(2L)).thenReturn(org.example.subcontracting.dto.response.SubcontractorScoreResponse.builder().score(80).build());
        when(subcontractRepository.countByMainFreelancerIdAndSubcontractorId(1L, 2L)).thenReturn(1L);

        ReflectionTestUtils.setField(aiMatchService, "anthropicApiKey", "");
        SubcontractMatchResponse heuristic = aiMatchService.matchSubcontractors(1L, List.of("Java"));
        assertThat(heuristic.getCandidates()).hasSize(1);
        assertThat(heuristic.getCandidates().get(0).getMatchScore()).isGreaterThan(0);

        ReflectionTestUtils.setField(aiMatchService, "anthropicApiKey", "key");
        ReflectionTestUtils.setField(aiMatchService, "anthropicUrl", "http://anthropic.local");
        ReflectionTestUtils.setField(aiMatchService, "anthropicModel", "claude");
        doReturn(ResponseEntity.ok("{\"content\":[{\"text\":\"{\\\"matches\\\":[{\\\"freelancerId\\\":2,\\\"matchScore\\\":91,\\\"matchReasons\\\":[\\\"Top\\\"],\\\"recommendation\\\":\\\"HIGHLY_RECOMMENDED\\\"}]}\"}]}"))
                .when(restTemplate).postForEntity(eq("http://anthropic.local"), any(), eq(String.class));
        SubcontractMatchResponse fromClaude = aiMatchService.matchSubcontractors(1L, List.of("Java"));
        assertThat(fromClaude.getCandidates()).hasSize(1);
        assertThat(fromClaude.getCandidates().get(0).getRecommendation()).isEqualTo("HIGHLY_RECOMMENDED");
    }

    @Test
    void aiDecisionAssistant_coversRiskTrapMatchAndFailurePrediction() {
        Subcontract sc = subcontract(10L, 1L, 2L);
        sc.setRequiredSkillsJson("[\"Java\",\"Spring\"]");
        when(subcontractRepository.findById(10L)).thenReturn(Optional.of(sc));
        when(subcontractRepository.findByMainFreelancerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                subcontract(11L, 1L, 2L), subcontract(12L, 1L, 2L), subcontract(13L, 1L, 2L), subcontract(14L, 1L, 3L)
        ));
        when(nestedAiMatchService.matchSubcontractors(1L, List.of("Java", "Spring")))
                .thenReturn(SubcontractMatchResponse.builder().candidates(List.of(
                        SubcontractMatchCandidateDto.builder().freelancerId(2L).fullName("Sara Dev").matchScore(88)
                                .trustScore(70).previousCollaborations(1L).recommendation("RECOMMENDED").matchReasons(List.of("skill fit")).build()
                )).build());

        var risk = aiDecisionAssistantService.riskScore(1L, 10L);
        assertThat(risk.getRiskScore()).isBetween(0, 100);
        assertThat(risk.getReferences()).isNotEmpty();

        var traps = aiDecisionAssistantService.trapsDetected(1L, 10L);
        assertThat(traps.getTraps()).isNotEmpty();

        var match = aiDecisionAssistantService.matchFreelancers(1L, 10L, 3);
        assertThat(match.getCandidates()).hasSize(1);

        var prediction = aiDecisionAssistantService.predictFailure(1L, 10L);
        assertThat(prediction.getFailureProbability()).isBetween(0, 100);
        assertThat(prediction.getMitigationPlan()).isNotEmpty();
    }

    @Test
    void aiDecisionAssistant_throwsOnUnauthorizedAccess() {
        Subcontract sc = subcontract(10L, 99L, 2L);
        when(subcontractRepository.findById(10L)).thenReturn(Optional.of(sc));
        assertThatThrownBy(() -> aiDecisionAssistantService.riskScore(1L, 10L))
                .isInstanceOf(ResponseStatusException.class);
    }

    private static UserRemoteDto user(Long id, String first, String last, String role, boolean active) {
        UserRemoteDto u = new UserRemoteDto();
        u.setId(id);
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(first.toLowerCase() + "@x.tn");
        u.setRole(role);
        u.setIsActive(active);
        return u;
    }

    private static PortfolioSkillDto skill(String name) {
        PortfolioSkillDto s = new PortfolioSkillDto();
        s.setName(name);
        return s;
    }

    private static Subcontract subcontract(Long id, Long mainId, Long subId) {
        Subcontract sc = new Subcontract();
        sc.setId(id);
        sc.setMainFreelancerId(mainId);
        sc.setSubcontractorId(subId);
        sc.setTitle("Build API");
        sc.setScope("Implement backend APIs and deliver tested endpoints with documentation.");
        sc.setCategory(SubcontractCategory.DEVELOPMENT);
        sc.setBudget(BigDecimal.valueOf(500));
        sc.setStatus(SubcontractStatus.CANCELLED);
        sc.setStartDate(LocalDate.now().minusDays(2));
        sc.setDeadline(LocalDate.now().plusDays(1));
        return sc;
    }
}
