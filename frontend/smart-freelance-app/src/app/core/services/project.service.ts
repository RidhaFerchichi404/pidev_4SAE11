import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';

const REQUEST_TIMEOUT_MS = 15_000;

const PROJECT_API = `${environment.apiGatewayUrl}/project/projects`;
const APPLICATIONS_API = `${environment.apiGatewayUrl}/project/applications`;

export interface Project {
  id?: number;
  clientId?: number;
  title: string;
  description: string;
  budget?: number;
  deadline?: string;
  status?: string;
  category?: string;
  skillsRequiered?: string | string[] | null;
  createdAt?: string;
  updatedAt?: string;
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

  private apiUrl = `${environment.apiGatewayUrl}/projects`;


  constructor(private http: HttpClient) {}

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

  getApplicationsByFreelancer(freelancerId: number): Observable<ProjectApplication[]> {
    return this.http.get<ProjectApplication[]>(`${APPLICATIONS_API}/freelance/${freelancerId}`).pipe(
      catchError(() => of([]))
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
}
