import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

const PLANNING_API = `${environment.apiGatewayUrl}/planning/api`;

/** Progress update submitted by a freelancer (matches backend ProgressUpdate). */
export interface ProgressUpdate {
  id: number;
  projectId: number;
  contractId: number | null;
  freelancerId: number;
  title: string;
  description: string | null;
  progressPercentage: number;
  createdAt: string;
  updatedAt: string;
  /** Optional next progress update due (synced to Google Calendar when enabled). */
  nextUpdateDue?: string | null;
  nextDueCalendarEventId?: string | null;
  comments?: ProgressComment[];
}

/** Comment on a progress update (client). Matches backend ProgressComment. */
export interface ProgressComment {
  id: number;
  progressUpdateId?: number;
  userId: number;
  message: string;
  createdAt: string;
}

/** Request body for create/update progress update. contractId is null while contract microservice is missing. */
export interface ProgressUpdateRequest {
  projectId: number;
  contractId: number | null;
  freelancerId: number;
  title: string;
  description?: string | null;
  progressPercentage: number;
  /** Optional next progress update due (synced to Google Calendar when enabled). ISO date-time. */
  nextUpdateDue?: string | null;
}

/** Request body for create/update comment. */
export interface ProgressCommentRequest {
  progressUpdateId: number;
  userId: number;
  message: string;
}

/** Spring Data Page response (GET /progress-updates returns this, not a raw array). */
export interface PageResponse<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  size?: number;
  number?: number;
}

/** Query params for filtered/paginated progress updates (planning microservice only). */
export interface ProgressUpdateFilterParams {
  page?: number;
  size?: number;
  sort?: string;
  projectId?: number | null;
  freelancerId?: number | null;
  contractId?: number | null;
  progressMin?: number | null;
  progressMax?: number | null;
  dateFrom?: string | null; // yyyy-MM-dd
  dateTo?: string | null;   // yyyy-MM-dd
  search?: string | null;   // title/description
}

/** Dashboard stats (planning microservice GET /progress-updates/stats/dashboard). */
export interface DashboardStatsDto {
  totalUpdates: number;
  totalComments: number;
  averageProgressPercentage: number | null;
  distinctProjectCount: number;
  distinctFreelancerCount: number;
}

/** Progress trend point (planning microservice GET /progress-updates/trend/project/:id). */
export interface ProgressTrendPointDto {
  date: string;       // yyyy-MM-dd
  progressPercentage: number;
}

/** Stalled project (planning microservice GET /progress-updates/stalled/projects). */
export interface StalledProjectDto {
  projectId: number;
  lastUpdateAt: string;
  lastProgressPercentage: number;
}

/** Freelancer activity ranking (planning microservice GET /progress-updates/rankings/freelancers). */
export interface FreelancerActivityDto {
  freelancerId: number;
  updateCount: number;
  commentCount: number;
}

/** Project activity ranking (planning microservice GET /progress-updates/rankings/projects). */
export interface ProjectActivityDto {
  projectId: number;
  updateCount: number;
}

/** Freelancer progress stats (GET /progress-updates/stats/freelancer/:id). */
export interface FreelancerProgressStatsDto {
  freelancerId: number;
  totalUpdates: number;
  totalComments: number;
  averageProgressPercentage: number | null;
  lastUpdateAt: string | null;
  updatesLast30Days: number;
}

/** Project progress stats (GET /progress-updates/stats/project/:id). */
export interface ProjectProgressStatsDto {
  projectId: number;
  updateCount: number;
  commentCount: number;
  currentProgressPercentage: number | null;
  firstUpdateAt: string | null;
  lastUpdateAt: string | null;
}

/** Validation response for a progress update (POST /progress-updates/validate). */
export interface ProgressUpdateValidationResponse {
  valid: boolean;
  minAllowed: number | null;
  provided: number | null;
  errors: string[];
}

/** Calendar event from Google Calendar (GET /api/calendar/events). */
export interface CalendarEventDto {
  id: string;
  summary: string;
  start: string | null;
  end: string | null;
  description: string | null;
}

/** Time-bounded project report (GET /progress-updates/stats/report). */
export interface ProgressReportDto {
  projectId: number;
  from: string;
  to: string;
  updateCount: number;
  commentCount: number;
  averageProgressPercentage: number | null;
  firstUpdateAt: string | null;
  lastUpdateAt: string | null;
}

/** Health payload for the Planning microservice (GET /planning/health). */
export interface PlanningHealthDatabase {
  status: string;
  progressUpdateCount?: number;
  error?: string;
}

export interface PlanningHealth {
  service: string;
  status: string;
  timestamp: string;
  database?: PlanningHealthDatabase;
}

@Injectable({ providedIn: 'root' })
export class PlanningService {
  constructor(private http: HttpClient) {}

  // ---------- Progress updates (Freelancer CRUD) ----------

  getAllProgressUpdates(): Observable<ProgressUpdate[]> {
    return this.http.get<PageResponse<ProgressUpdate>>(`${PLANNING_API}/progress-updates`).pipe(
      map((page) => (page?.content && Array.isArray(page.content) ? page.content : [])),
      catchError(() => of([]))
    );
  }

  /**
   * AJAX filtered + paginated list (planning microservice only).
   * Uses GET /progress-updates with query params.
   */
  getFilteredProgressUpdates(params: ProgressUpdateFilterParams): Observable<PageResponse<ProgressUpdate>> {
    let query = new URLSearchParams();
    if (params.page != null) query.set('page', String(params.page));
    if (params.size != null) query.set('size', String(params.size));
    if (params.sort != null && params.sort.trim()) query.set('sort', params.sort.trim());
    if (params.projectId != null) query.set('projectId', String(params.projectId));
    if (params.freelancerId != null) query.set('freelancerId', String(params.freelancerId));
    if (params.contractId != null) query.set('contractId', String(params.contractId));
    if (params.progressMin != null) query.set('progressMin', String(params.progressMin));
    if (params.progressMax != null) query.set('progressMax', String(params.progressMax));
    if (params.dateFrom != null && params.dateFrom.trim()) query.set('dateFrom', params.dateFrom.trim());
    if (params.dateTo != null && params.dateTo.trim()) query.set('dateTo', params.dateTo.trim());
    if (params.search != null && params.search.trim()) query.set('search', params.search.trim());
    const qs = query.toString();
    const url = qs ? `${PLANNING_API}/progress-updates?${qs}` : `${PLANNING_API}/progress-updates`;
    return this.http.get<PageResponse<ProgressUpdate>>(url).pipe(
      map((p) => ({
        content: p?.content ?? [],
        totalElements: p?.totalElements ?? 0,
        totalPages: p?.totalPages ?? 0,
        size: p?.size ?? 20,
        number: p?.number ?? 0,
      })),
      catchError(() => of({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 }))
    );
  }

  /** Dashboard statistics (planning microservice only). */
  getDashboardStats(): Observable<DashboardStatsDto | null> {
    return this.http.get<DashboardStatsDto>(`${PLANNING_API}/progress-updates/stats/dashboard`).pipe(
      catchError(() => of(null))
    );
  }

  /** Progress trend by project (planning microservice only). Optional from/to as yyyy-MM-dd. */
  getProgressTrendByProject(projectId: number, from?: string | null, to?: string | null): Observable<ProgressTrendPointDto[]> {
    let query = new URLSearchParams();
    if (from?.trim()) query.set('from', from.trim());
    if (to?.trim()) query.set('to', to.trim());
    const qs = query.toString();
    const url = qs ? `${PLANNING_API}/progress-updates/trend/project/${projectId}?${qs}` : `${PLANNING_API}/progress-updates/trend/project/${projectId}`;
    return this.http.get<ProgressTrendPointDto[]>(url).pipe(catchError(() => of([])));
  }

  /** Stalled projects (planning microservice only). */
  getStalledProjects(daysWithoutUpdate: number = 7): Observable<StalledProjectDto[]> {
    return this.http.get<StalledProjectDto[]>(`${PLANNING_API}/progress-updates/stalled/projects?daysWithoutUpdate=${daysWithoutUpdate}`).pipe(
      catchError(() => of([]))
    );
  }

  /** Top freelancers by activity (planning microservice only). */
  getFreelancersByActivity(limit: number = 10): Observable<FreelancerActivityDto[]> {
    return this.http.get<FreelancerActivityDto[]>(`${PLANNING_API}/progress-updates/rankings/freelancers?limit=${limit}`).pipe(
      catchError(() => of([]))
    );
  }

  /** Most active projects (planning microservice only). Optional from/to as yyyy-MM-dd. */
  getMostActiveProjects(limit: number = 10, from?: string | null, to?: string | null): Observable<ProjectActivityDto[]> {
    let query = new URLSearchParams();
    query.set('limit', String(limit));
    if (from?.trim()) query.set('from', from.trim());
    if (to?.trim()) query.set('to', to.trim());
    return this.http.get<ProjectActivityDto[]>(`${PLANNING_API}/progress-updates/rankings/projects?${query.toString()}`).pipe(
      catchError(() => of([]))
    );
  }

  /** Stats by freelancer (planning microservice only). */
  getStatsByFreelancer(freelancerId: number): Observable<FreelancerProgressStatsDto | null> {
    return this.http.get<FreelancerProgressStatsDto>(`${PLANNING_API}/progress-updates/stats/freelancer/${freelancerId}`).pipe(
      catchError(() => of(null))
    );
  }

  /** Stats by project (planning microservice only). */
  getStatsByProject(projectId: number): Observable<ProjectProgressStatsDto | null> {
    return this.http.get<ProjectProgressStatsDto>(`${PLANNING_API}/progress-updates/stats/project/${projectId}`).pipe(
      catchError(() => of(null))
    );
  }

  /** Stats by contract (planning microservice only). */
  getStatsByContract(contractId: number): Observable<Record<string, unknown> | null> {
    return this.http.get<Record<string, unknown>>(`${PLANNING_API}/progress-updates/stats/contract/${contractId}`).pipe(
      catchError(() => of(null))
    );
  }

  getProgressUpdateById(id: number): Observable<ProgressUpdate | null> {
    return this.http.get<ProgressUpdate>(`${PLANNING_API}/progress-updates/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  getProgressUpdatesByProjectId(projectId: number): Observable<ProgressUpdate[]> {
    return this.http.get<ProgressUpdate[]>(`${PLANNING_API}/progress-updates/project/${projectId}`).pipe(
      catchError(() => of([]))
    );
  }

  getProgressUpdatesByFreelancerId(freelancerId: number): Observable<ProgressUpdate[]> {
    return this.http.get<ProgressUpdate[]>(`${PLANNING_API}/progress-updates/freelancer/${freelancerId}`).pipe(
      catchError(() => of([]))
    );
  }

  /** Latest progress update for a given project (planning microservice only). */
  getLatestProgressUpdateByProject(projectId: number): Observable<ProgressUpdate | null> {
    const url = `${PLANNING_API}/progress-updates/latest?projectId=${projectId}`;
    return this.http.get<ProgressUpdate>(url).pipe(
      catchError((err) => {
        if (err?.status === 404) return of(null);
        return of(null);
      })
    );
  }

  /** Latest progress update for a given freelancer (planning microservice only). */
  getLatestProgressUpdateByFreelancer(freelancerId: number): Observable<ProgressUpdate | null> {
    const url = `${PLANNING_API}/progress-updates/latest?freelancerId=${freelancerId}`;
    return this.http.get<ProgressUpdate>(url).pipe(
      catchError((err) => {
        if (err?.status === 404) return of(null);
        return of(null);
      })
    );
  }

  /** Latest progress update for a given contract (planning microservice only). */
  getLatestProgressUpdateByContract(contractId: number): Observable<ProgressUpdate | null> {
    const url = `${PLANNING_API}/progress-updates/latest?contractId=${contractId}`;
    return this.http.get<ProgressUpdate>(url).pipe(
      catchError((err) => {
        if (err?.status === 404) return of(null);
        return of(null);
      })
    );
  }

  createProgressUpdate(request: ProgressUpdateRequest): Observable<ProgressUpdate> {
    return this.http.post<ProgressUpdate>(`${PLANNING_API}/progress-updates`, request).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  updateProgressUpdate(id: number, request: ProgressUpdateRequest): Observable<ProgressUpdate | null> {
    return this.http.put<ProgressUpdate>(`${PLANNING_API}/progress-updates/${id}`, request).pipe(
      catchError(() => of(null))
    );
  }

  deleteProgressUpdate(id: number): Observable<boolean> {
    return this.http.delete(`${PLANNING_API}/progress-updates/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  /** Get minimum allowed progress percentage for the next update of a project. */
  getNextAllowedProgressPercentage(projectId: number): Observable<number> {
    return this.http
      .get<{ projectId: number; minAllowed: number }>(
        `${PLANNING_API}/progress-updates/next-allowed-percentage`,
        { params: { projectId: String(projectId) } }
      )
      .pipe(
        map((res) => (typeof res?.minAllowed === 'number' ? res.minAllowed : 0)),
        catchError(() => of(0))
      );
  }

  /** Validate a progress update without persisting (backend applies same rules as create/update). */
  validateProgressUpdate(request: ProgressUpdateRequest): Observable<ProgressUpdateValidationResponse> {
    return this.http
      .post<ProgressUpdateValidationResponse>(`${PLANNING_API}/progress-updates/validate`, request)
      .pipe(
        catchError(() =>
          of({
            valid: false,
            minAllowed: null,
            provided: request.progressPercentage ?? null,
            errors: ['Validation failed. Please try again later.'],
          })
        )
      );
  }

  /** List calendar events, optionally scoped to the current user (userId + role). */
  getCalendarEvents(params?: {
    timeMin?: string;
    timeMax?: string;
    calendarId?: string;
    userId?: number | null;
    role?: string | null;
  }): Observable<CalendarEventDto[]> {
    const q = new URLSearchParams();
    if (params?.timeMin) q.set('timeMin', params.timeMin);
    if (params?.timeMax) q.set('timeMax', params.timeMax);
    if (params?.calendarId) q.set('calendarId', params.calendarId);
    if (params?.userId != null) q.set('userId', String(params.userId));
    if (params?.role) q.set('role', params.role);
    const url = q.toString() ? `${PLANNING_API}/calendar/events?${q.toString()}` : `${PLANNING_API}/calendar/events`;
    return this.http.get<CalendarEventDto[]>(url).pipe(catchError(() => of([])));
  }

  /** Ensure project deadline is in the calendar for the freelancer; notifies when first synced. */
  syncProjectDeadlineToCalendar(projectId: number, freelancerId: number): Observable<void> {
    return this.http.post<void>(
      `${PLANNING_API}/calendar/sync-project-deadline?projectId=${projectId}&freelancerId=${freelancerId}`,
      {}
    ).pipe(catchError(() => of(undefined)));
  }

  // ---------- Progress comments (Client CRUD) ----------

  getAllComments(): Observable<ProgressComment[]> {
    return this.http.get<ProgressComment[]>(`${PLANNING_API}/progress-comments`).pipe(
      catchError(() => of([]))
    );
  }

  /** Paginated comments for admin/moderation (planning microservice only). */
  getCommentsPaged(page: number, size: number = 20): Observable<PageResponse<ProgressComment>> {
    const query = new URLSearchParams();
    query.set('page', String(page));
    query.set('size', String(size));
    const url = `${PLANNING_API}/progress-comments?${query.toString()}`;
    return this.http.get<PageResponse<ProgressComment>>(url).pipe(
      map((p) => ({
        content: p?.content ?? [],
        totalElements: p?.totalElements ?? 0,
        totalPages: p?.totalPages ?? 0,
        size: p?.size ?? size,
        number: p?.number ?? page,
      })),
      catchError(() => of({ content: [], totalElements: 0, totalPages: 0, size, number: page }))
    );
  }

  getCommentById(id: number): Observable<ProgressComment | null> {
    return this.http.get<ProgressComment>(`${PLANNING_API}/progress-comments/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  getCommentsByProgressUpdateId(progressUpdateId: number): Observable<ProgressComment[]> {
    return this.http.get<ProgressComment[]>(`${PLANNING_API}/progress-comments/progress-update/${progressUpdateId}`).pipe(
      catchError(() => of([]))
    );
  }

  /** All comments authored by a specific user (planning microservice only). */
  getCommentsByUserId(userId: number): Observable<ProgressComment[]> {
    return this.http.get<ProgressComment[]>(`${PLANNING_API}/progress-comments/by-user/${userId}`).pipe(
      catchError(() => of([]))
    );
  }

  createComment(request: ProgressCommentRequest): Observable<ProgressComment | null> {
    return this.http.post<ProgressComment>(`${PLANNING_API}/progress-comments`, request).pipe(
      catchError(() => of(null))
    );
  }

  updateComment(id: number, request: Pick<ProgressCommentRequest, 'message'>): Observable<ProgressComment | null> {
    return this.http.put<ProgressComment>(`${PLANNING_API}/progress-comments/${id}`, request).pipe(
      catchError(() => of(null))
    );
  }

  /** Partially update comment (currently only message) using PATCH. */
  patchComment(id: number, payload: { message?: string }): Observable<ProgressComment | null> {
    return this.http.patch<ProgressComment>(`${PLANNING_API}/progress-comments/${id}`, payload).pipe(
      catchError(() => of(null))
    );
  }

  deleteComment(id: number): Observable<boolean> {
    return this.http.delete(`${PLANNING_API}/progress-comments/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  // ---------- Aggregated / Reporting / Health ----------

  /** Projects that are due or overdue for an update (alias to stalled projects). */
  getDueOrOverdueProjects(daysWithoutUpdate: number = 7): Observable<StalledProjectDto[]> {
    // Use the existing stalled/projects endpoint behind the scenes for maximum compatibility.
    return this.http
      .get<StalledProjectDto[]>(`${PLANNING_API}/progress-updates/stalled/projects?daysWithoutUpdate=${daysWithoutUpdate}`)
      .pipe(catchError(() => of([])));
  }

  /** Project-level report for a period. If from/to omitted, backend defaults to last 30 days. */
  getProjectReport(projectId: number, from?: string | null, to?: string | null): Observable<ProgressReportDto | null> {
    const query = new URLSearchParams();
    query.set('projectId', String(projectId));
    if (from?.trim()) query.set('from', from.trim());
    if (to?.trim()) query.set('to', to.trim());
    const url = `${PLANNING_API}/progress-updates/stats/report?${query.toString()}`;
    return this.http.get<ProgressReportDto>(url).pipe(
      catchError(() => of(null))
    );
  }

  /** Download a CSV export of progress updates matching the provided filters. */
  downloadProgressExport(params: Omit<ProgressUpdateFilterParams, 'page' | 'size' | 'sort'> & { format?: string }): Observable<Blob> {
    const query = new URLSearchParams();
    if (params.projectId != null) query.set('projectId', String(params.projectId));
    if (params.freelancerId != null) query.set('freelancerId', String(params.freelancerId));
    if (params.contractId != null) query.set('contractId', String(params.contractId));
    if (params.progressMin != null) query.set('progressMin', String(params.progressMin));
    if (params.progressMax != null) query.set('progressMax', String(params.progressMax));
    if (params.dateFrom != null && params.dateFrom.trim()) query.set('dateFrom', params.dateFrom.trim());
    if (params.dateTo != null && params.dateTo.trim()) query.set('dateTo', params.dateTo.trim());
    if (params.search != null && params.search.trim()) query.set('search', params.search.trim());
    query.set('format', (params.format ?? 'csv').trim().toLowerCase());
    const url = `${PLANNING_API}/progress-updates/export?${query.toString()}`;
    return this.http.get(url, { responseType: 'blob' });
  }

  /** Lightweight health check for the Planning microservice (via API Gateway). */
  getPlanningHealth(): Observable<PlanningHealth | null> {
    // Use the dedicated Planning health endpoint exposed by the microservice itself.
    return this.http.get<PlanningHealth>(`${PLANNING_API}/planning/health`).pipe(
      catchError(() => of(null))
    );
  }
}
