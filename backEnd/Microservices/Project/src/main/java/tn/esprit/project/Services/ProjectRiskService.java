package tn.esprit.project.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tn.esprit.project.Client.ReviewFeignClient;
import tn.esprit.project.Client.TaskStatsFeignClient;
import tn.esprit.project.Client.dto.ReviewFeignDto;
import tn.esprit.project.Client.dto.TaskStatsFeignDto;
import tn.esprit.project.Dto.response.ProjectClientSegmentResponse;
import tn.esprit.project.Dto.response.ProjectRiskResponse;
import tn.esprit.project.Dto.response.ProjectSegmentationOverviewResponse;
import tn.esprit.project.Dto.response.ProjectSatisfactionResponse;
import tn.esprit.project.Entities.Enums.ProjectStatus;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Repository.ProjectRepository;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectRiskService {

    private final ProjectRepository projectRepository;
    private final TaskStatsFeignClient taskStatsFeignClient;
    private final ReviewFeignClient reviewFeignClient;
    private final WebClient mlInferenceWebClient;

    @Value("${ml.risk.enabled:true}")
    private boolean mlRiskEnabled;

    @Value("${ml.risk.use-task-aggregates:true}")
    private boolean useTaskAggregates;

    @Value("${ml.risk.use-review-aggregates:true}")
    private boolean useReviewAggregates;

    private record FeatureContext(Project project, TaskStatsFeignDto taskStats, List<ReviewFeignDto> reviews, boolean taskOk, boolean reviewOk) {}

    public ProjectRiskResponse getProjectRisk(Long projectId) {
        if (!mlRiskEnabled) {
            return ProjectRiskResponse.builder()
                    .available(false)
                    .aggregatesUsed(false)
                    .riskPercent(0)
                    .successProbability(0)
                    .reasons(List.of())
                    .message("Project risk ML is disabled")
                    .build();
        }

        Optional<FeatureContext> maybeContext = loadFeatureContext(projectId);
        if (maybeContext.isEmpty()) {
            return ProjectRiskResponse.builder()
                    .available(false)
                    .aggregatesUsed(false)
                    .riskPercent(0)
                    .successProbability(0)
                    .reasons(List.of())
                    .message("Project not found")
                    .build();
        }

        FeatureContext context = maybeContext.get();
        Map<String, Object> features = buildFeatures(context.project(), context.taskStats(), context.reviews());

        try {
            Map<String, Object> body = Map.of("features", features);
            Map<?, ?> resp = mlInferenceWebClient
                    .post()
                    .uri("/predict")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (resp == null) {
                return unavailable("Empty response from ML service");
            }
            resp = unwrapMlEnvelope(resp);
            double successProb = toDouble(firstPresent(resp, "successProbability", "success_probability"));
            double riskPct = toDouble(firstPresent(resp, "riskPercent", "risk_percent"));
            List<String> reasons = mergeReasonLists(
                    extractReasons(firstPresent(resp, "reasons", "riskReasons", "explanations")),
                    extractReasons(firstPresent(resp, "reason", "explanation", "details"))
            );
            if (reasons.isEmpty()) {
                reasons = fallbackRiskReasons(features, successProb, riskPct);
            }

            return ProjectRiskResponse.builder()
                    .available(true)
                    .aggregatesUsed(context.taskOk() || context.reviewOk())
                    .successProbability(successProb)
                    .riskPercent(riskPct)
                    .reasons(reasons)
                    .build();
        } catch (WebClientResponseException e) {
            return unavailable("ML inference error: " + e.getStatusCode());
        } catch (Exception e) {
            return unavailable("ML inference unreachable: " + e.getMessage());
        }
    }

    public ProjectSatisfactionResponse getProjectSatisfaction(Long projectId) {
        if (!mlRiskEnabled) {
            return ProjectSatisfactionResponse.builder()
                    .available(false)
                    .aggregatesUsed(false)
                    .satisfactionScore(0)
                    .satisfactionPercent(0)
                    .reasons(List.of())
                    .message("Project risk ML is disabled")
                    .build();
        }

        Optional<FeatureContext> maybeContext = loadFeatureContext(projectId);
        if (maybeContext.isEmpty()) {
            return ProjectSatisfactionResponse.builder()
                    .available(false)
                    .aggregatesUsed(false)
                    .satisfactionScore(0)
                    .satisfactionPercent(0)
                    .reasons(List.of())
                    .message("Project not found")
                    .build();
        }

        FeatureContext context = maybeContext.get();
        Map<String, Object> features = buildFeatures(context.project(), context.taskStats(), context.reviews());

        try {
            Map<String, Object> body = Map.of("features", features);
            Map<?, ?> resp = mlInferenceWebClient
                    .post()
                    .uri("/predict-satisfaction")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (resp == null) {
                return unavailableSatisfaction("Empty response from ML service");
            }
            resp = unwrapMlEnvelope(resp);
            double score = toDouble(firstPresent(resp, "satisfactionScore", "satisfaction_score"));
            double percent = toDouble(firstPresent(resp, "satisfactionPercent", "satisfaction_percent"));
            List<String> reasons = mergeReasonLists(
                    extractReasons(firstPresent(resp, "reasons", "riskReasons", "explanations")),
                    extractReasons(firstPresent(resp, "reason", "explanation", "details"))
            );

            return ProjectSatisfactionResponse.builder()
                    .available(true)
                    .aggregatesUsed(context.taskOk() || context.reviewOk())
                    .satisfactionScore(score)
                    .satisfactionPercent(percent)
                    .reasons(reasons)
                    .build();
        } catch (WebClientResponseException e) {
            return unavailableSatisfaction("ML inference error: " + e.getStatusCode());
        } catch (Exception e) {
            return unavailableSatisfaction("ML inference unreachable: " + e.getMessage());
        }
    }

    public ProjectClientSegmentResponse getClientSegment(Long clientId) {
        if (!mlRiskEnabled) {
            return unavailableSegment(clientId, "Project risk ML is disabled");
        }
        Map<String, Object> features = buildClientSegmentationFeatures(clientId);
        if (features.isEmpty()) {
            return unavailableSegment(clientId, "Client not found");
        }
        try {
            Map<String, Object> body = Map.of("features", features);
            Map<?, ?> resp = mlInferenceWebClient
                    .post()
                    .uri("/predict-segment")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (resp == null) {
                return unavailableSegment(clientId, "Empty response from ML service");
            }
            resp = unwrapMlEnvelope(resp);
            int segmentId = toInt(firstPresent(resp, "segmentId", "segment_id"));
            String label = toStringValue(firstPresent(resp, "segmentLabel", "segment_label"), "Segment " + segmentId);
            double confidence = toDouble(firstPresent(resp, "confidence"));
            List<String> reasons = mergeReasonLists(
                    extractReasons(firstPresent(resp, "reasons", "riskReasons", "explanations")),
                    extractReasons(firstPresent(resp, "reason", "explanation", "details"))
            );
            return ProjectClientSegmentResponse.builder()
                    .clientId(clientId)
                    .segmentId(segmentId)
                    .segmentLabel(label)
                    .confidence(confidence)
                    .reasons(reasons)
                    .available(true)
                    .build();
        } catch (WebClientResponseException e) {
            return unavailableSegment(clientId, "ML inference error: " + e.getStatusCode());
        } catch (Exception e) {
            return unavailableSegment(clientId, "ML inference unreachable: " + e.getMessage());
        }
    }

    public ProjectSegmentationOverviewResponse getSegmentationOverview() {
        if (!mlRiskEnabled) {
            return unavailableOverview("Project risk ML is disabled");
        }
        List<Long> clientIds = projectRepository.findDistinctClientIds();
        if (clientIds.isEmpty()) {
            return ProjectSegmentationOverviewResponse.builder()
                    .segments(List.of())
                    .summaryCounts(Map.of())
                    .available(true)
                    .build();
        }
        List<Map<String, Object>> clientsPayload = new ArrayList<>(clientIds.size());
        for (Long clientId : clientIds) {
            Map<String, Object> features = buildClientSegmentationFeatures(clientId);
            if (features.isEmpty()) {
                continue;
            }
            clientsPayload.add(Map.of("clientId", clientId, "features", features));
        }
        try {
            Map<String, Object> body = Map.of("clients", clientsPayload);
            Map<?, ?> resp = mlInferenceWebClient
                    .post()
                    .uri("/segment-clients")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (resp == null) {
                return unavailableOverview("Empty response from ML service");
            }
            resp = unwrapMlEnvelope(resp);
            List<ProjectClientSegmentResponse> segments = extractSegmentList(resp.get("segments"));
            Map<String, Integer> summary = extractSummaryCounts(resp.get("summaryCounts"));
            if (summary.isEmpty()) {
                summary = buildSummaryFromSegments(segments);
            }
            return ProjectSegmentationOverviewResponse.builder()
                    .segments(segments)
                    .summaryCounts(summary)
                    .available(true)
                    .build();
        } catch (WebClientResponseException e) {
            return unavailableOverview("ML inference error: " + e.getStatusCode());
        } catch (Exception e) {
            return unavailableOverview("ML inference unreachable: " + e.getMessage());
        }
    }

    private static ProjectRiskResponse unavailable(String message) {
        return ProjectRiskResponse.builder()
                .available(false)
                .aggregatesUsed(false)
                .riskPercent(0)
                .successProbability(0)
                .reasons(List.of())
                .message(message)
                .build();
    }

    private static ProjectSatisfactionResponse unavailableSatisfaction(String message) {
        return ProjectSatisfactionResponse.builder()
                .available(false)
                .aggregatesUsed(false)
                .satisfactionScore(0)
                .satisfactionPercent(0)
                .reasons(List.of())
                .message(message)
                .build();
    }

    private static ProjectClientSegmentResponse unavailableSegment(Long clientId, String message) {
        return ProjectClientSegmentResponse.builder()
                .clientId(clientId)
                .segmentId(-1)
                .segmentLabel("Unavailable")
                .confidence(0)
                .reasons(List.of())
                .available(false)
                .message(message)
                .build();
    }

    private static ProjectSegmentationOverviewResponse unavailableOverview(String message) {
        return ProjectSegmentationOverviewResponse.builder()
                .segments(List.of())
                .summaryCounts(Map.of())
                .available(false)
                .message(message)
                .build();
    }

    private Optional<FeatureContext> loadFeatureContext(Long projectId) {
        Optional<Project> opt = projectRepository.findById(projectId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        Project p = opt.get();

        boolean taskOk = false;
        boolean reviewOk = false;

        TaskStatsFeignDto taskStats = null;
        if (useTaskAggregates) {
            try {
                taskStats = taskStatsFeignClient.getStatsByProject(projectId);
                taskOk = taskStats != null;
            } catch (Exception ignored) {
                taskOk = false;
            }
        }

        List<ReviewFeignDto> reviews = List.of();
        if (useReviewAggregates) {
            try {
                reviews = reviewFeignClient.getReviewsByProject(projectId);
                reviewOk = true;
            } catch (Exception ignored) {
                reviewOk = false;
                reviews = List.of();
            }
        }
        return Optional.of(new FeatureContext(p, taskStats, reviews, taskOk, reviewOk));
    }

    private static Map<?, ?> unwrapMlEnvelope(Map<?, ?> resp) {
        Object nested = firstPresent(resp, "data", "body", "payload", "result");
        if (nested instanceof Map<?, ?> m
                && (m.containsKey("successProbability")
                || m.containsKey("success_probability")
                || m.containsKey("riskPercent")
                || m.containsKey("risk_percent")
                || m.containsKey("reasons")
                || m.containsKey("satisfactionScore")
                || m.containsKey("satisfaction_score"))) {
            return m;
        }
        return resp;
    }

    private static Object firstPresent(Map<?, ?> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k) && map.get(k) != null) {
                return map.get(k);
            }
        }
        return null;
    }

    private static List<String> mergeReasonLists(List<String> a, List<String> b) {
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        List<String> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    private static List<String> fallbackRiskReasons(Map<String, Object> features, double successProb, double riskPct) {
        List<String> out = new ArrayList<>();
        double successPct = successProb;
        if (successProb >= 0 && successProb <= 1.0) {
            successPct = successProb * 100.0;
        }
        out.add(String.format(
                "The model combines category, budget, timeline, and (when available) task and review signals into one score — "
                        + "about %.1f%% estimated success vs %.1f%% failure risk for this project.",
                successPct, riskPct));

        Object cat = features.get("category");
        if (cat != null && !cat.toString().isBlank()) {
            out.add("Category \"" + cat + "\" is one of the stronger categorical signals in the classifier.");
        }
        Object budget = features.get("budget_usd");
        if (budget instanceof Number b && b.doubleValue() > 0) {
            out.add(String.format("Budget around $%.0f is weighed against typical patterns for similar postings.", b.doubleValue()));
        }
        Object dd = features.get("deadline_days");
        if (dd instanceof Number d && d.longValue() != 0) {
            out.add(String.format("About %d days from project creation to deadline affects how tight the delivery window looks to the model.", d.longValue()));
        }
        Object overrun = features.get("deadline_overrun_days");
        if (overrun instanceof Number o && o.longValue() > 0) {
            out.add(String.format("The deadline is past by about %d day(s), which usually pushes the risk estimate upward.", o.longValue()));
        }
        return out;
    }

    private static List<String> extractReasons(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof String s) {
            s = s.trim();
            return s.isEmpty() ? List.of() : List.of(s);
        }
        if (raw instanceof JsonNode node) {
            return extractReasonsFromJsonNode(node);
        }
        if (raw instanceof Object[] arr) {
            return extractReasons(Arrays.asList(arr));
        }
        if (raw instanceof Collection<?> col && !(raw instanceof List<?>)) {
            return extractReasons(new ArrayList<>(col));
        }
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                if (o instanceof String str) {
                    str = str.trim();
                    if (!str.isEmpty()) {
                        out.add(str);
                    }
                    continue;
                }
                if (o instanceof Map<?, ?> m) {
                    String line = reasonLineFromMap(m);
                    if (line != null) {
                        out.add(line);
                    }
                    continue;
                }
                if (o instanceof JsonNode jn) {
                    out.addAll(extractReasons(jn));
                    continue;
                }
                String t = o.toString().trim();
                if (!t.isEmpty() && !t.startsWith("{") && !t.startsWith("[")) {
                    out.add(t);
                }
            }
            return out;
        }
        return List.of();
    }

    private static List<String> extractReasonsFromJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isTextual()) {
            String s = node.asText("").trim();
            return s.isEmpty() ? List.of() : List.of(s);
        }
        if (node.isArray()) {
            List<String> out = new ArrayList<>();
            for (JsonNode el : node) {
                out.addAll(extractReasons(el));
            }
            return out;
        }
        if (node.isObject()) {
            String line = null;
            JsonNode text = node.get("text");
            if (text != null && text.isTextual()) {
                line = text.asText().trim();
            }
            if ((line == null || line.isEmpty()) && node.has("message") && node.get("message").isTextual()) {
                line = node.get("message").asText().trim();
            }
            if ((line == null || line.isEmpty()) && node.has("reason") && node.get("reason").isTextual()) {
                line = node.get("reason").asText().trim();
            }
            return (line == null || line.isEmpty()) ? List.of() : List.of(line);
        }
        return List.of();
    }

    private static String reasonLineFromMap(Map<?, ?> m) {
        Object text = m.get("text");
        if (text != null) {
            String s = text.toString().trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        Object message = m.get("message");
        if (message != null) {
            String s = message.toString().trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        Object reason = m.get("reason");
        if (reason != null) {
            String s = reason.toString().trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    private Map<String, Object> buildClientSegmentationFeatures(Long clientId) {
        if (clientId == null) {
            return Map.of();
        }
        List<Project> projects = projectRepository.findByClientId(clientId);
        if (projects.isEmpty()) {
            return Map.of();
        }
        int total = projects.size();
        long completed = projects.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();
        long cancelled = projects.stream().filter(p -> p.getStatus() == ProjectStatus.CANCELLED).count();
        double successRate = total == 0 ? 0.0 : (double) completed / total;
        double completionRatio = total == 0 ? 0.0 : (double) (completed + (total - completed - cancelled) * 0.5) / total;
        double avgComplexity = projects.stream()
                .mapToDouble(p -> {
                    int n = p.getSkillIds() == null ? 0 : p.getSkillIds().size();
                    return Math.min(1.0, n / 10.0);
                })
                .average()
                .orElse(0.0);
        BigDecimal avgBudget = projectRepository.averageBudgetByClientId(clientId);

        Map<String, Object> m = new HashMap<>();
        m.put("client_type", "sme");
        m.put("communication_score", 6.5);
        m.put("strictness_score", 5.5);
        m.put("payment_delay_days", 7.0);
        m.put("repeat_hiring_rate", Math.min(1.0, total / 10.0));
        m.put("avg_budget_usd", avgBudget != null ? avgBudget.doubleValue() : 0.0);
        m.put("project_count_history", total);
        m.put("mean_completion_ratio", completionRatio);
        m.put("mean_delay_days", 0.0);
        m.put("project_success_rate", successRate);
        m.put("mean_budget_usd", avgBudget != null ? avgBudget.doubleValue() : 0.0);
        m.put("avg_complexity", avgComplexity);
        m.put("total_projects", total);
        return m;
    }

    private static List<ProjectClientSegmentResponse> extractSegmentList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<ProjectClientSegmentResponse> out = new ArrayList<>(list.size());
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) {
                continue;
            }
            Long clientId = toLong(firstPresent(m, "clientId", "client_id"));
            int segmentId = toInt(firstPresent(m, "segmentId", "segment_id"));
            String segmentLabel = toStringValue(firstPresent(m, "segmentLabel", "segment_label"), "Segment " + segmentId);
            double confidence = toDouble(firstPresent(m, "confidence"));
            List<String> reasons = mergeReasonLists(
                    extractReasons(firstPresent(m, "reasons", "riskReasons", "explanations")),
                    extractReasons(firstPresent(m, "reason", "explanation", "details"))
            );
            out.add(ProjectClientSegmentResponse.builder()
                    .clientId(clientId)
                    .segmentId(segmentId)
                    .segmentLabel(segmentLabel)
                    .confidence(confidence)
                    .reasons(reasons)
                    .available(true)
                    .build());
        }
        return out;
    }

    private static Map<String, Integer> extractSummaryCounts(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String key = e.getKey().toString();
            int value = toInt(e.getValue());
            out.put(key, Math.max(0, value));
        }
        return out;
    }

    private static Map<String, Integer> buildSummaryFromSegments(List<ProjectClientSegmentResponse> segments) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (ProjectClientSegmentResponse s : segments) {
            String key = s.getSegmentLabel() == null ? ("Segment " + s.getSegmentId()) : s.getSegmentLabel();
            out.put(key, out.getOrDefault(key, 0) + 1);
        }
        return out;
    }

    private Map<String, Object> buildFeatures(Project p, TaskStatsFeignDto taskStats, List<ReviewFeignDto> reviews) {
        Map<String, Object> m = new HashMap<>();
        Long clientId = p.getClientId();
        if (clientId != null) {
            m.put("client_id", clientId);
            m.put("project_count_history", projectRepository.countByClientId(clientId));
            BigDecimal avgB = projectRepository.averageBudgetByClientId(clientId);
            m.put("avg_budget_usd", avgB != null ? avgB.doubleValue() : 0.0);
        }

        m.put("category", Optional.ofNullable(p.getCategory()).orElse(""));
        m.put("budget_usd", p.getBudget() != null ? p.getBudget().doubleValue() : 0.0);

        long deadlineDays = 0L;
        if (p.getCreatedAt() != null && p.getDeadline() != null) {
            deadlineDays = ChronoUnit.DAYS.between(p.getCreatedAt().toLocalDate(), p.getDeadline().toLocalDate());
        }
        m.put("deadline_days", deadlineDays);

        long createdDayIndex = p.getCreatedAt() != null ? p.getCreatedAt().toLocalDate().toEpochDay() : 0L;
        m.put("created_day_index", createdDayIndex);

        int nSkills = p.getSkillIds() == null ? 0 : p.getSkillIds().size();
        m.put("complexity_score", Math.min(1.0, nSkills / 10.0));

        if (taskStats != null) {
            m.put("task_count", taskStats.getTotalTasks());
            m.put("task_completed_count", taskStats.getDoneCount());
            m.put("task_delayed_count", taskStats.getOverdueCount());
            m.put("task_blocked_count", 0L);
            m.put("completion_ratio", taskStats.getCompletionPercentage() / 100.0);
        }

        m.put("avg_task_delay_days", 0.0);

        long overrun = 0L;
        if (p.getDeadline() != null && p.getStatus() != ProjectStatus.COMPLETED) {
            LocalDate d = p.getDeadline().toLocalDate();
            if (d.isBefore(LocalDate.now())) {
                overrun = ChronoUnit.DAYS.between(d, LocalDate.now());
            }
        }
        m.put("deadline_overrun_days", overrun);

        if (reviews != null && !reviews.isEmpty()) {
            double avgRating = reviews.stream()
                    .map(ReviewFeignDto::getRating)
                    .filter(r -> r != null)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
            m.put("avg_review_rating", avgRating);
            m.put("review_count", reviews.size());
        }

        return m;
    }

    private static double toDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static Long toLong(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String toStringValue(Object o, String fallback) {
        if (o == null) {
            return fallback;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? fallback : s;
    }
}
