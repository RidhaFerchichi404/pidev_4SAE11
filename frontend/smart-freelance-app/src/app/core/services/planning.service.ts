import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
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
}

/** Request body for create/update comment. */
export interface ProgressCommentRequest {
  progressUpdateId: number;
  userId: number;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class PlanningService {
  constructor(private http: HttpClient) {}

  // ---------- Progress updates (Freelancer CRUD) ----------

  getAllProgressUpdates(): Observable<ProgressUpdate[]> {
    return this.http.get<ProgressUpdate[]>(`${PLANNING_API}/progress-updates`).pipe(
      catchError(() => of([]))
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

  createProgressUpdate(request: ProgressUpdateRequest): Observable<ProgressUpdate | null> {
    return this.http.post<ProgressUpdate>(`${PLANNING_API}/progress-updates`, request).pipe(
      catchError(() => of(null))
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

  // ---------- Progress comments (Client CRUD) ----------

  getAllComments(): Observable<ProgressComment[]> {
    return this.http.get<ProgressComment[]>(`${PLANNING_API}/progress-comments`).pipe(
      catchError(() => of([]))
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

  deleteComment(id: number): Observable<boolean> {
    return this.http.delete(`${PLANNING_API}/progress-comments/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false))
    );
  }
}
