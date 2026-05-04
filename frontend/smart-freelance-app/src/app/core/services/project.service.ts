import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Skill } from './portfolio.service';

const REQUEST_TIMEOUT_MS = 15_000;

const PROJECT_API = `${environment.apiGatewayUrl}/project/projects`;
const APPLICATIONS_API = `${environment.apiGatewayUrl}/project/applications`;

export interface Project {
  id?: number;
  clientId?: number;
  freelancerId?: number;
  title: string;
  description: string;
  budget?: number;
  deadline?: string;
  status?: string;
  category?: string;
  skillIds?: number[];
  skills?: Skill[];
  createdAt?: string;
  updatedAt?: string;
}

export interface ProjectRisk {
  riskPercent: number;
  successProbability: number;
  /** Explanations from ML (counterfactual vs baseline), when present. */
  reasons?: string[];
  aggregatesUsed: boolean;
  available: boolean;
  message?: string;
}

export interface ProjectSatisfaction {
  satisfactionScore: number;
  satisfactionPercent: number;
  reasons?: string[];
  aggregatesUsed: boolean;
  available: boolean;
  message?: string;
}

export interface ClientSegment {
  clientId?: number;
  segmentId: number;
  segmentLabel: string;
  confidence: number;
  reasons?: string[];
  available: boolean;
  message?: string;
}

export interface SegmentationOverview {
  segments: ClientSegment[];
  summaryCounts: Record<string, number>;
  available: boolean;
  message?: string;
}

export interface ProjectApplication {
  id: number;
  projectId: number;
  freelanceId: number;
  coverLetter?: string;
  proposedPrice?: number;
  proposedDuration?: number;
  status?: string;
  appliedAt?: string;
  respondedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class ProjectService {
  constructor(private readonly http: HttpClient) {}

  private normalizeRiskReasons(raw: any): string[] {
    const fromArray = (value: unknown): string[] =>
      Array.isArray(value)
        ? value
            .filter((item) => item != null)
            .map((item) => String(item).trim())
            .filter((item) => item.length > 0)
        : [];

    const normalized = [
      ...fromArray(raw?.reasons),
      ...fromArray(raw?.reason),
      ...fromArray(raw?.riskReasons),
      ...fromArray(raw?.explanations),
    ];

    if (normalized.length > 0) return normalized;

    // Accept APIs that return one text reason instead of an array.
    const single = [raw?.reason, raw?.reasons, raw?.explanation, raw?.details]
      .find((value) => typeof value === 'string' && value.trim().length > 0);
    return single ? [String(single).trim()] : [];
  }

  private unwrapNestedPayload(raw: any): any {
    if (raw == null || typeof raw !== 'object' || Array.isArray(raw)) {
      return raw;
    }
    const nested = raw.data ?? raw.body ?? raw.payload ?? raw.result;
    if (
      nested &&
      typeof nested === 'object' &&
      !Array.isArray(nested) &&
      ('riskPercent' in nested ||
        'risk_percentage' in nested ||
        'successProbability' in nested ||
        'success_probability' in nested ||
        'reasons' in nested ||
        'satisfactionScore' in nested)
    ) {
      return nested;
    }
    return raw;
  }

  private normalizeProjectRisk(raw: any): ProjectRisk {
    raw = this.unwrapNestedPayload(raw);
    const successProbabilityRaw = raw?.successProbability ?? raw?.success_probability ?? 0;
    const riskPercentRaw = raw?.riskPercent ?? raw?.risk_percentage ?? 0;
    const successProbability = Number(successProbabilityRaw);
    const riskPercent = Number(riskPercentRaw);

    return {
      riskPercent: Number.isFinite(riskPercent) ? riskPercent : 0,
      successProbability: Number.isFinite(successProbability) ? successProbability : 0,
      reasons: this.normalizeRiskReasons(raw),
      aggregatesUsed: Boolean(raw?.aggregatesUsed ?? raw?.aggregates_used),
      available: Boolean(raw?.available ?? true),
      message: typeof raw?.message === 'string' ? raw.message : undefined,
    };
  }

  private normalizeProjectSatisfaction(raw: any): ProjectSatisfaction {
    raw = this.unwrapNestedPayload(raw);
    const scoreRaw = raw?.satisfactionScore ?? raw?.satisfaction_score ?? 0;
    const percentRaw = raw?.satisfactionPercent ?? raw?.satisfaction_percent ?? 0;
    const score = Number(scoreRaw);
    const percent = Number(percentRaw);

    return {
      satisfactionScore: Number.isFinite(score) ? score : 0,
      satisfactionPercent: Number.isFinite(percent) ? percent : 0,
      reasons: this.normalizeRiskReasons(raw),
      aggregatesUsed: Boolean(raw?.aggregatesUsed ?? raw?.aggregates_used),
      available: Boolean(raw?.available ?? true),
      message: typeof raw?.message === 'string' ? raw.message : undefined,
    };
  }

  private normalizeClientSegment(raw: any): ClientSegment {
    raw = this.unwrapNestedPayload(raw);
    const segmentId = Number(raw?.segmentId ?? raw?.segment_id ?? -1);
    const confidence = Number(raw?.confidence ?? 0);
    const clientId = Number(raw?.clientId ?? raw?.client_id);
    return {
      clientId: Number.isFinite(clientId) ? clientId : undefined,
      segmentId: Number.isFinite(segmentId) ? segmentId : -1,
      segmentLabel: String(raw?.segmentLabel ?? raw?.segment_label ?? 'Unavailable'),
      confidence: Number.isFinite(confidence) ? confidence : 0,
      reasons: this.normalizeRiskReasons(raw),
      available: Boolean(raw?.available ?? true),
      message: typeof raw?.message === 'string' ? raw.message : undefined,
    };
  }

  private normalizeSegmentationOverview(raw: any): SegmentationOverview {
    raw = this.unwrapNestedPayload(raw);
    const segmentsRaw = Array.isArray(raw?.segments) ? raw.segments : [];
    const segments = segmentsRaw.map((entry: any) => this.normalizeClientSegment(entry));
    const summaryRaw = raw?.summaryCounts ?? raw?.summary_counts;
    const summaryCounts: Record<string, number> = {};
    if (summaryRaw && typeof summaryRaw === 'object' && !Array.isArray(summaryRaw)) {
      Object.entries(summaryRaw).forEach(([k, v]) => {
        const n = Number(v);
        if (Number.isFinite(n)) summaryCounts[k] = n;
      });
    }
    return {
      segments,
      summaryCounts,
      available: Boolean(raw?.available ?? true),
      message: typeof raw?.message === 'string' ? raw.message : undefined,
    };
  }

  getById(id: number): Observable<Project | null> {
    return this.http.get<Project>(`${PROJECT_API}/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  getByClientId(clientId: number): Observable<Project[]> {
    return this.http.get<Project[]>(`${PROJECT_API}/client/${clientId}`).pipe(
      timeout(REQUEST_TIMEOUT_MS)
    );
  }

  /**
   * Projets publiés par ce client avec au moins une candidature acceptée
   * (vous avez accepté un freelancer sur l’annonce).
   */
  getProjectsAsClientWithAcceptedFreelancer(clientId: number): Observable<Project[]> {
    return this.http
      .get<Project[]>(`${PROJECT_API}/client/${clientId}/with-accepted-freelancer`)
      .pipe(timeout(REQUEST_TIMEOUT_MS));
  }

  /** Projets où ce freelancer a une candidature acceptée (missions qu'il exécute). */
  getProjectsForFreelancer(freelancerId: number): Observable<Project[]> {
    return this.http.get<Project[]>(`${PROJECT_API}/freelancer/${freelancerId}`).pipe(
      timeout(REQUEST_TIMEOUT_MS)
    );
  }
  /** Get projects for a freelancer (uses recommended endpoint; backend has no /freelancer/{id}). */
  getByFreelancerId(id: number): Observable<Project[]> {
    return this.http.get<Project[]>(`${PROJECT_API}/recommended`, { params: { userId: id } }).pipe(
      catchError(() => of([]))
    );
  }

  getApplicationsByFreelancer(freelancerId: number): Observable<ProjectApplication[]> {
    return this.http.get<ProjectApplication[]>(`${APPLICATIONS_API}/freelance/${freelancerId}`).pipe(
      catchError(() => of([]))
    );
  }

  exportProjectsPdf(): Observable<Blob> {
    return this.http.get(
      `${PROJECT_API}/export/pdf`,
      { responseType: 'blob' }
    );
  }

  /** List all projects (backend: GET /projects/list). */
  getAllProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${PROJECT_API}/list`).pipe(
      timeout(REQUEST_TIMEOUT_MS)
    );
  }

  /** Get one project by id. Alias for getById for compatibility. */
  getProjectById(id: string | number): Observable<Project | null> {
    return this.getById(Number(id));
  }

  /** Create project (backend: POST /projects/add). */
  createProject(project: Partial<Project>): Observable<Project | null> {
    return this.http.post<Project>(`${PROJECT_API}/add`, project).pipe(
      catchError(() => of(null))
    );
  }

  /** Update project (backend: PUT /projects/update). */
  updateProject(project: Partial<Project>): Observable<Project | null> {
    return this.http.put<Project>(`${PROJECT_API}/update`, project).pipe(
      catchError(() => of(null))
    );
  }

  /** Delete project (backend: DELETE /projects/{id}). */
  deleteProject(id: string | number): Observable<boolean> {
    return this.http.delete(`${PROJECT_API}/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }

  getRecommendedProjects(userId: number) {
    return this.http.get<Project[]>(
      `${PROJECT_API}/recommended`,
      {
        params: { userId }
      }
    );
  }

  getProjectStatistics() {
    return this.http.get<any>(
      `${PROJECT_API}/statistics`
    );
  }

  /** ML failure risk (%) and success probability (gateway: /project/projects/{id}/risk). */
  getProjectRisk(id: number): Observable<ProjectRisk | null> {
    return this.http.get<any>(`${PROJECT_API}/${id}/risk`).pipe(
      map((res) => this.normalizeProjectRisk(res)),
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }

  /** Predicted client satisfaction (/10 and %) for a project. */
  getProjectSatisfaction(id: number): Observable<ProjectSatisfaction | null> {
    return this.http.get<any>(`${PROJECT_API}/${id}/satisfaction`).pipe(
      map((res) => this.normalizeProjectSatisfaction(res)),
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }

  getClientSegment(clientId: number): Observable<ClientSegment | null> {
    return this.http.get<any>(`${PROJECT_API}/client/${clientId}/segment`).pipe(
      map((res) => this.normalizeClientSegment(res)),
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }

  getSegmentationOverview(): Observable<SegmentationOverview | null> {
    return this.http.get<any>(`${PROJECT_API}/segmentation/overview`).pipe(
      map((res) => this.normalizeSegmentationOverview(res)),
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null))
    );
  }
}
