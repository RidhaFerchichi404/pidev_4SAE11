# Planning Microservice – Advanced Methods Plan

This document specifies advanced REST endpoints and backend logic for the **Planning** microservice. All logic uses only **ProgressUpdate** and **ProgressComment** entities (no calls to User, Project, or Contract services).

---

## 1. Statistics

### 1.1 Stats by freelancer

- **Endpoint:** `GET /api/progress-updates/stats/freelancer/{freelancerId}`
- **Response DTO:** `FreelancerProgressStatsDto`
  - `Long freelancerId`
  - `long totalUpdates`
  - `long totalComments` (comments on updates by this freelancer)
  - `Double averageProgressPercentage` (average of progressPercentage across their updates)
  - `Integer currentProgressPercentage` (optional: max or latest % if you aggregate by project; can omit for “by freelancer” and keep per-project elsewhere)
  - `LocalDateTime lastUpdateAt`
  - `long updatesLast30Days` (activity score)
- **Repository:** Use existing `findByFreelancerId`. Count comments via `ProgressCommentRepository` (comments where `progressUpdate.freelancerId` = given id, or count by progressUpdate ids).
- **Service:** `getProgressStatisticsByFreelancer(Long freelancerId): FreelancerProgressStatsDto`
- **Controller:** Single GET method returning `FreelancerProgressStatsDto`.

### 1.2 Stats by project

- **Endpoint:** `GET /api/progress-updates/stats/project/{projectId}`
- **Response DTO:** `ProjectProgressStatsDto`
  - `Long projectId`
  - `long updateCount`
  - `long commentCount`
  - `Integer currentProgressPercentage` (latest or max progressPercentage for this project)
  - `LocalDateTime firstUpdateAt`
  - `LocalDateTime lastUpdateAt`
- **Repository:** Use existing `findByProjectId`. For comments: load updates by projectId, then count comments for those update ids.
- **Service:** `getProgressStatisticsByProject(Long projectId): ProjectProgressStatsDto`
- **Controller:** Single GET method returning `ProjectProgressStatsDto`.

### 1.3 Stats by contract

- **Endpoint:** `GET /api/progress-updates/stats/contract/{contractId}`
- **Response DTO:** `ContractProgressStatsDto` (same shape as project: contractId, updateCount, commentCount, currentProgressPercentage, firstUpdateAt, lastUpdateAt)
- **Repository:** Use existing `findByContractId`; comment count from comments on those updates.
- **Service:** `getProgressStatisticsByContract(Long contractId): ContractProgressStatsDto`
- **Controller:** Single GET returning `ContractProgressStatsDto`.

### 1.4 Global / dashboard stats

- **Endpoint:** `GET /api/progress-updates/stats/dashboard`
- **Response DTO:** `DashboardStatsDto`
  - `long totalUpdates`
  - `long totalComments`
  - `Double averageProgressPercentage` (over all updates)
  - `long distinctProjectCount`
  - `long distinctFreelancerCount`
- **Repository:** Aggregate over all `ProgressUpdate` and `ProgressComment`; distinct counts on projectId and freelancerId.
- **Service:** `getDashboardStatistics(): DashboardStatsDto`
- **Controller:** Single GET returning `DashboardStatsDto`.

---

## 2. Progress trend (time-series)

### 2.1 Progress trend by project

- **Endpoint:** `GET /api/progress-updates/trend/project/{projectId}?from=yyyy-MM-dd&to=yyyy-MM-dd`
- **Response DTO:** `List<ProgressTrendPointDto>`
  - `LocalDate date`
  - `Integer progressPercentage` (e.g. latest or max for that day)
- **Logic:** Query updates by projectId and createdAt/updatedAt between from and to; group by date (day) and take latest or max progress per day.
- **Repository:** Add `List<ProgressUpdate> findByProjectIdAndCreatedAtBetween(Long projectId, LocalDateTime from, LocalDateTime to);` or use a custom query.
- **Service:** `getProgressTrendByProject(Long projectId, LocalDate from, LocalDate to): List<ProgressTrendPointDto>`
- **Controller:** GET with path variable and request params `from`, `to`.

---

## 3. Filtering and search (paginated list)

### 3.1 Paginated list with filters

- **Endpoint:** `GET /api/progress-updates?page=0&size=20&sort=createdAt,desc&projectId=&freelancerId=&contractId=&progressMin=&progressMax=&dateFrom=&dateTo=`
- **Query params (all optional):** `page`, `size`, `sort`, `projectId`, `freelancerId`, `contractId`, `progressMin`, `progressMax`, `dateFrom` (ISO date), `dateTo` (ISO date).
- **Response:** `Page<ProgressUpdate>` or `Page<ProgressUpdateSummaryDto>` (id, projectId, freelancerId, title, progressPercentage, createdAt, commentCount).
- **Repository:** Extend `ProgressUpdateRepository` with `JpaSpecification` or `Querydsl`, or add a single method with `Pageable` and all filter params (building predicates in service or repository).
- **Service:** `findAllFiltered(Optional<Long> projectId, Optional<Long> freelancerId, Optional<Long> contractId, Optional<Integer> progressMin, Optional<Integer> progressMax, Optional<LocalDate> dateFrom, Optional<LocalDate> dateTo, Pageable pageable): Page<ProgressUpdate>`
- **Controller:** Single GET mapping; parse query params and delegate to service.

### 3.2 Text search (title + description)

- **Option A:** Same endpoint as 3.1 with extra param `search=keyword`. Filter where title or description contains keyword (LIKE %keyword% or use JPA Specification).
- **Option B:** Separate endpoint `GET /api/progress-updates/search?q=keyword&page=0&size=20` that returns the same paginated shape.
- **Repository:** Add predicate for `title` and `description` (ignore case LIKE).
- **Service:** Include search in the same filtered method or a dedicated `search(String q, Pageable pageable)`.
- **Controller:** One GET; if using Option A, add `search` to the same handler as 3.1.

---

## 4. Stalled / at-risk projects

### 4.1 Projects with no recent update

- **Endpoint:** `GET /api/progress-updates/stalled/projects?daysWithoutUpdate=7`
- **Response DTO:** `List<StalledProjectDto>`
  - `Long projectId`
  - `LocalDateTime lastUpdateAt`
  - `Integer lastProgressPercentage`
- **Logic:** For each projectId that has at least one update, compute max(updatedAt); return those where (now - max(updatedAt)) > daysWithoutUpdate days.
- **Repository:** Custom query or native: group by projectId, max(updatedAt), and optionally last progress (e.g. the progressPercentage of the update with max updatedAt). Filter in service or SQL by days.
- **Service:** `getProjectIdsWithStalledProgress(int daysWithoutUpdate): List<StalledProjectDto>`
- **Controller:** GET with request param `daysWithoutUpdate` (default e.g. 7).

---

## 5. Rankings / top lists

### 5.1 Top freelancers by activity

- **Endpoint:** `GET /api/progress-updates/rankings/freelancers?limit=10`
- **Response DTO:** `List<FreelancerActivityDto>`
  - `Long freelancerId`
  - `long updateCount`
  - `long commentCount` (comments on their updates)
- **Repository:** Query ProgressUpdate grouped by freelancerId with count; optionally join/compute comment count per freelancer.
- **Service:** `getFreelancersByActivity(int limit): List<FreelancerActivityDto>`
- **Controller:** GET with request param `limit` (default 10).

### 5.2 Most active projects

- **Endpoint:** `GET /api/progress-updates/rankings/projects?limit=10&from=yyyy-MM-dd&to=yyyy-MM-dd` (from/to optional)
- **Response DTO:** `List<ProjectActivityDto>`
  - `Long projectId`
  - `long updateCount`
- **Repository:** Group by projectId, count updates, optionally filter by createdAt between from and to.
- **Service:** `getMostActiveProjects(int limit, Optional<LocalDate> from, Optional<LocalDate> to): List<ProjectActivityDto>`
- **Controller:** GET with optional `limit`, `from`, `to`.

---

## 6. DTOs to create (summary)

| DTO | Fields |
|-----|--------|
| `FreelancerProgressStatsDto` | freelancerId, totalUpdates, totalComments, averageProgressPercentage, lastUpdateAt, updatesLast30Days |
| `ProjectProgressStatsDto` | projectId, updateCount, commentCount, currentProgressPercentage, firstUpdateAt, lastUpdateAt |
| `ContractProgressStatsDto` | contractId, updateCount, commentCount, currentProgressPercentage, firstUpdateAt, lastUpdateAt |
| `DashboardStatsDto` | totalUpdates, totalComments, averageProgressPercentage, distinctProjectCount, distinctFreelancerCount |
| `ProgressTrendPointDto` | date, progressPercentage |
| `ProgressUpdateSummaryDto` (optional) | id, projectId, freelancerId, title, progressPercentage, createdAt, commentCount |
| `StalledProjectDto` | projectId, lastUpdateAt, lastProgressPercentage |
| `FreelancerActivityDto` | freelancerId, updateCount, commentCount |
| `ProjectActivityDto` | projectId, updateCount |

---

## 7. Repository methods to add

- `List<ProgressUpdate> findByProjectIdAndCreatedAtBetween(Long projectId, LocalDateTime from, LocalDateTime to);`
- Filtered page: use `JpaSpecification<ProgressUpdate>` with dynamic predicates for projectId, freelancerId, contractId, progressMin, progressMax, dateFrom, dateTo, and optional search on title/description.
- Stalled: custom query or native SQL grouping by projectId with max(updatedAt) and filter by days.
- Rankings: count group by freelancerId; count group by projectId (with optional date range).

---

## 8. Implementation order (suggested)

1. **DTOs** – Create all DTOs in a `dto` package (or subpackage `dto/advanced`).
2. **Statistics** – Stats by freelancer, project, contract, dashboard (repository usage + service + controller).
3. **Trend** – Progress trend by project (repository + service + controller).
4. **Filtering and search** – Paginated filtered list + optional search param (JpaSpecification or custom repo + service + controller).
5. **Stalled** – Stalled projects endpoint (custom query + service + controller).
6. **Rankings** – Top freelancers and top projects (custom queries + service + controller).

---

## 9. Subagent tasks (mapping)

- **Subagent 1 – DTOs:** Create all DTOs listed in section 6 under `src/main/java/com/esprit/planning/dto/`.
- **Subagent 2 – Statistics:** Implement 1.1–1.4 (repository usage, service methods, controller endpoints).
- **Subagent 3 – Trend:** Implement section 2 (repository method, service, controller).
- **Subagent 4 – Filtering and search:** Implement section 3 (JpaSpecification or custom repo, service, controller).
- **Subagent 5 – Stalled and rankings:** Implement sections 4 and 5 (custom queries, services, controllers).
