package com.esprit.planning.controller;

import com.esprit.planning.dto.FreelancerActivityDto;
import com.esprit.planning.dto.ProgressTrendPointDto;
import com.esprit.planning.dto.ProgressUpdateRequest;
import com.esprit.planning.dto.ProgressUpdateValidationResponse;
import com.esprit.planning.dto.ProgressUpdateWithCommentsDto;
import com.esprit.planning.dto.ProjectActivityDto;
import com.esprit.planning.dto.StalledProjectDto;
import com.esprit.planning.entity.ProgressComment;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.exception.ProgressCannotDecreaseException;
import com.esprit.planning.service.ProgressCommentService;
import com.esprit.planning.service.ProgressUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/progress-updates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Progress Updates", description = "Create and manage progress updates for projects")
public class ProgressUpdateController {

    private final ProgressUpdateService progressUpdateService;
    private final ProgressCommentService progressCommentService;

    @Value("${welcome.message}")
    private String welcomeMessage;

    @GetMapping("/welcome")
    public String welcome() {
        return welcomeMessage;
    }

    @GetMapping
    @Operation(
            summary = "Paginated list with filters and search",
            description = "Returns a paginated list of progress updates. All query params are optional. Use search for title/description (case-insensitive)."
    )
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<Page<ProgressUpdate>> getFiltered(
            @Parameter(description = "Page index (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort (e.g. createdAt,desc)") @RequestParam(required = false) String sort,
            @Parameter(description = "Filter by project ID") @RequestParam(required = false) Long projectId,
            @Parameter(description = "Filter by freelancer ID") @RequestParam(required = false) Long freelancerId,
            @Parameter(description = "Filter by contract ID") @RequestParam(required = false) Long contractId,
            @Parameter(description = "Minimum progress percentage (0-100)") @RequestParam(required = false) Integer progressMin,
            @Parameter(description = "Maximum progress percentage (0-100)") @RequestParam(required = false) Integer progressMax,
            @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Search in title and description (case-insensitive)") @RequestParam(required = false) String search) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<ProgressUpdate> result = progressUpdateService.findAllFiltered(
                Optional.ofNullable(projectId),
                Optional.ofNullable(freelancerId),
                Optional.ofNullable(contractId),
                Optional.ofNullable(progressMin),
                Optional.ofNullable(progressMax),
                Optional.ofNullable(dateFrom),
                Optional.ofNullable(dateTo),
                Optional.ofNullable(search),
                pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export")
    @Operation(
            summary = "Export progress updates",
            description = "Exports progress updates matching the same filters as the list endpoint. Currently supports CSV via the format query parameter (format=csv)."
    )
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<String> export(
            @Parameter(description = "Filter by project ID") @RequestParam(required = false) Long projectId,
            @Parameter(description = "Filter by freelancer ID") @RequestParam(required = false) Long freelancerId,
            @Parameter(description = "Filter by contract ID") @RequestParam(required = false) Long contractId,
            @Parameter(description = "Minimum progress percentage (0-100)") @RequestParam(required = false) Integer progressMin,
            @Parameter(description = "Maximum progress percentage (0-100)") @RequestParam(required = false) Integer progressMax,
            @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Search in title and description (case-insensitive)") @RequestParam(required = false) String search,
            @Parameter(description = "Export format (currently only 'csv' is supported)") @RequestParam(required = false, defaultValue = "csv") String format
    ) {
        // For now we always return CSV regardless of the requested format to avoid 400s from clients.

        List<ProgressUpdate> result = progressUpdateService.findAllFilteredForExport(
                Optional.ofNullable(projectId),
                Optional.ofNullable(freelancerId),
                Optional.ofNullable(contractId),
                Optional.ofNullable(progressMin),
                Optional.ofNullable(progressMax),
                Optional.ofNullable(dateFrom),
                Optional.ofNullable(dateTo),
                Optional.ofNullable(search)
        );

        StringBuilder sb = new StringBuilder();
        sb.append("id,projectId,contractId,freelancerId,title,description,progressPercentage,createdAt,updatedAt,commentCount\n");
        for (ProgressUpdate update : result) {
            long commentCount = update.getComments() != null ? update.getComments().size() : 0;
            sb.append(csv(update.getId()))
                    .append(',').append(csv(update.getProjectId()))
                    .append(',').append(csv(update.getContractId()))
                    .append(',').append(csv(update.getFreelancerId()))
                    .append(',').append(csv(update.getTitle()))
                    .append(',').append(csv(update.getDescription()))
                    .append(',').append(csv(update.getProgressPercentage()))
                    .append(',').append(csv(update.getCreatedAt()))
                    .append(',').append(csv(update.getUpdatedAt()))
                    .append(',').append(csv(commentCount))
                    .append('\n');
        }

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"progress-updates-export.csv\"")
                .body(sb.toString());
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String str = String.valueOf(value);
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"");
            return "\"" + str + "\"";
        }
        return str;
    }

    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        if (parts.length == 1) {
            return Sort.by(parts[0].trim());
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, parts[0].trim());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get progress update by ID", description = "Returns a single progress update by its id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Progress update not found", content = @Content)
    })
    public ResponseEntity<ProgressUpdate> getById(
            @Parameter(description = "Progress update ID", example = "1", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(progressUpdateService.findById(id));
    }

    @GetMapping("/{id}/with-comments")
    @Operation(
            summary = "Get progress update with comments",
            description = "Returns a single progress update together with all its comments in one call."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Progress update not found", content = @Content)
    })
    public ResponseEntity<ProgressUpdateWithCommentsDto> getByIdWithComments(
            @Parameter(description = "Progress update ID", example = "1", required = true) @PathVariable Long id) {
        ProgressUpdate update = progressUpdateService.findById(id);
        List<ProgressComment> comments = progressCommentService.findByProgressUpdateId(id);
        ProgressUpdateWithCommentsDto dto = ProgressUpdateWithCommentsDto.builder()
                .progressUpdate(update)
                .comments(comments)
                .build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List by project", description = "Returns all progress updates for the given project.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<ProgressUpdate>> getByProjectId(
            @Parameter(description = "Project ID", example = "1", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(progressUpdateService.findByProjectId(projectId));
    }

    @GetMapping("/contract/{contractId}")
    @Operation(summary = "List by contract", description = "Returns all progress updates for the given contract.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<ProgressUpdate>> getByContractId(
            @Parameter(description = "Contract ID", example = "1", required = true) @PathVariable Long contractId) {
        return ResponseEntity.ok(progressUpdateService.findByContractId(contractId));
    }

    @GetMapping("/freelancer/{freelancerId}")
    @Operation(summary = "List by freelancer", description = "Returns all progress updates submitted by the given freelancer.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<ProgressUpdate>> getByFreelancerId(
            @Parameter(description = "Freelancer ID", example = "10", required = true) @PathVariable Long freelancerId) {
        return ResponseEntity.ok(progressUpdateService.findByFreelancerId(freelancerId));
    }

    @GetMapping("/latest")
    @Operation(
            summary = "Get latest progress update",
            description = "Returns the latest progress update for a project, freelancer or contract. Exactly one of projectId, freelancerId or contractId must be provided."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content),
            @ApiResponse(responseCode = "404", description = "No matching progress update found", content = @Content)
    })
    public ResponseEntity<?> getLatest(
            @Parameter(description = "Project ID")
            @RequestParam(value = "projectId", required = false) Long projectId,
            @Parameter(description = "Freelancer ID")
            @RequestParam(value = "freelancerId", required = false) Long freelancerId,
            @Parameter(description = "Contract ID")
            @RequestParam(value = "contractId", required = false) Long contractId
    ) {
        int nonNullCount = (projectId != null ? 1 : 0)
                + (freelancerId != null ? 1 : 0)
                + (contractId != null ? 1 : 0);
        if (nonNullCount != 1) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Exactly one of projectId, freelancerId or contractId must be provided"
            ));
        }

        Optional<ProgressUpdate> result;
        if (projectId != null) {
            result = progressUpdateService.findLatestByProjectId(projectId);
        } else if (freelancerId != null) {
            result = progressUpdateService.findLatestByFreelancerId(freelancerId);
        } else {
            result = progressUpdateService.findLatestByContractId(contractId);
        }

        return result.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No progress update found for the provided criteria")));
    }

    @GetMapping("/trend/project/{projectId}")
    @Operation(summary = "Progress trend by project", description = "Returns progress trend points (date, progress %) for the project in the given date range. If from/to omitted, uses last 30 days.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<ProgressTrendPointDto>> getProgressTrendByProject(
            @Parameter(description = "Project ID", example = "1", required = true) @PathVariable Long projectId,
            @Parameter(description = "Start date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate from,
            @Parameter(description = "End date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate to) {
        LocalDate toDate = to != null ? to : LocalDate.now();
        LocalDate fromDate = from != null ? from : toDate.minusDays(30);
        return ResponseEntity.ok(progressUpdateService.getProgressTrendByProject(projectId, fromDate, toDate));
    }

    @GetMapping("/stalled/projects")
    @Operation(summary = "Stalled projects", description = "Returns projects with no progress update in the last N days (default 7).")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<StalledProjectDto>> getStalledProjects(
            @Parameter(description = "Number of days without update to consider stalled", example = "7") @RequestParam(defaultValue = "7") int daysWithoutUpdate) {
        return ResponseEntity.ok(progressUpdateService.getProjectIdsWithStalledProgress(daysWithoutUpdate));
    }

    @GetMapping("/due-or-overdue")
    @Operation(
            summary = "Due or overdue projects",
            description = "Alias for stalled projects: returns projects with no progress update in the last N days (default 7). Useful for reminders and notifications."
    )
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<StalledProjectDto>> getDueOrOverdueProjects(
            @Parameter(description = "Number of days without update to consider due/overdue", example = "7") @RequestParam(defaultValue = "7") int daysWithoutUpdate) {
        return ResponseEntity.ok(progressUpdateService.getProjectIdsWithStalledProgress(daysWithoutUpdate));
    }

    @GetMapping("/rankings/freelancers")
    @Operation(summary = "Top freelancers by activity", description = "Returns freelancers ranked by progress update count, with comment count on their updates.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<FreelancerActivityDto>> getFreelancersByActivity(
            @Parameter(description = "Maximum number of freelancers to return", example = "10") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(progressUpdateService.getFreelancersByActivity(limit));
    }

    @GetMapping("/rankings/projects")
    @Operation(summary = "Most active projects", description = "Returns projects ranked by progress update count, optionally filtered by date range.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<List<ProjectActivityDto>> getMostActiveProjects(
            @Parameter(description = "Maximum number of projects to return", example = "10") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "From date (yyyy-MM-dd), optional") @RequestParam(required = false) LocalDate from,
            @Parameter(description = "To date (yyyy-MM-dd), optional") @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(progressUpdateService.getMostActiveProjects(
                limit, Optional.ofNullable(from), Optional.ofNullable(to)));
    }

    @PostMapping
    @Operation(summary = "Create progress update", description = "Creates a new progress update. Do not send id, createdAt or updatedAt.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = ProgressUpdate.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    public ResponseEntity<ProgressUpdate> create(@RequestBody ProgressUpdateRequest request) {
        ProgressUpdate entity = ProgressUpdate.builder()
                .projectId(request.getProjectId())
                .contractId(request.getContractId())
                .freelancerId(request.getFreelancerId())
                .title(request.getTitle())
                .description(request.getDescription())
                .progressPercentage(request.getProgressPercentage())
                .nextUpdateDue(request.getNextUpdateDue())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(progressUpdateService.create(entity));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update progress update", description = "Updates an existing progress update by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ProgressUpdate.class))),
            @ApiResponse(responseCode = "404", description = "Progress update not found", content = @Content)
    })
    public ResponseEntity<ProgressUpdate> update(
            @Parameter(description = "Progress update ID", example = "1", required = true) @PathVariable Long id,
            @RequestBody ProgressUpdateRequest request) {
        ProgressUpdate entity = ProgressUpdate.builder()
                .projectId(request.getProjectId())
                .contractId(request.getContractId())
                .freelancerId(request.getFreelancerId())
                .title(request.getTitle())
                .description(request.getDescription())
                .progressPercentage(request.getProgressPercentage())
                .nextUpdateDue(request.getNextUpdateDue())
                .build();
        return ResponseEntity.ok(progressUpdateService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete progress update", description = "Deletes a progress update and its comments.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No content"),
            @ApiResponse(responseCode = "404", description = "Progress update not found", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Progress update ID", example = "1", required = true) @PathVariable Long id) {
        progressUpdateService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/next-allowed-percentage")
    @Operation(
            summary = "Get next allowed progress percentage",
            description = "Returns the minimum allowed progress percentage for the next update of a project, based on the 'cannot decrease' rule."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Missing projectId", content = @Content)
    })
    public ResponseEntity<?> getNextAllowedPercentage(
            @Parameter(description = "Project ID", required = true) @RequestParam(required = false) Long projectId
    ) {
        if (projectId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "projectId is required"));
        }
        Integer minAllowed = progressUpdateService.getNextAllowedPercentageForProject(projectId);
        return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "minAllowed", minAllowed
        ));
    }

    @PostMapping("/validate")
    @Operation(
            summary = "Validate progress update without persisting",
            description = "Validates a progress update request against backend rules (required fields, percentage range, cannot decrease rule) without saving it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation result", content = @Content(schema = @Schema(implementation = ProgressUpdateValidationResponse.class)))
    })
    public ResponseEntity<ProgressUpdateValidationResponse> validate(@RequestBody ProgressUpdateRequest request) {
        ProgressUpdateValidationResponse response = progressUpdateService.validate(request);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(ProgressCannotDecreaseException.class)
    public ResponseEntity<Map<String, Object>> handleProgressCannotDecrease(ProgressCannotDecreaseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", ex.getMessage(),
                "minAllowed", ex.getMinAllowed(),
                "provided", ex.getProvided()
        ));
    }
}
