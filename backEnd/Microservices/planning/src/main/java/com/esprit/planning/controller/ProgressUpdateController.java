package com.esprit.planning.controller;

import com.esprit.planning.dto.FreelancerActivityDto;
import com.esprit.planning.dto.ProgressTrendPointDto;
import com.esprit.planning.dto.ProgressUpdateRequest;
import com.esprit.planning.dto.ProjectActivityDto;
import com.esprit.planning.dto.StalledProjectDto;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.exception.ProgressCannotDecreaseException;
import com.esprit.planning.service.ProgressUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @ExceptionHandler(ProgressCannotDecreaseException.class)
    public ResponseEntity<Map<String, Object>> handleProgressCannotDecrease(ProgressCannotDecreaseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", ex.getMessage(),
                "minAllowed", ex.getMinAllowed(),
                "provided", ex.getProvided()
        ));
    }
}
