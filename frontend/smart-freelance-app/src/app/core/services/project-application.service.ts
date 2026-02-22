import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from './user.service';

const REQUEST_TIMEOUT_MS = 15_000;
const APPLICATIONS_API = `${environment.apiGatewayUrl}/project/applications`;

export interface ProjectApplication {
  id?: number;
  projectId: number;
  freelanceId: number;
  coverLetter?: string;
  proposedPrice?: number;
  proposedDuration?: number;
  status?: string;
  appliedAt?: string;
  respondedAt?: string;

  project?: {
    id: number;
    clientId: number;
    title: string;
    description: string;
    budget: number;
    deadline: string;
    status: string;
    category: string;
    skillsRequiered: string;
  };

}

@Injectable({
  providedIn: 'root',
})
export class ProjectApplicationService {
  constructor(private http: HttpClient) {}

  addApplication(application: Partial<ProjectApplication>): Observable<ProjectApplication | null> {
    return this.http.post<ProjectApplication>(APPLICATIONS_API, application).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null)),
    );
  }



  updateApplication(
    application: Partial<ProjectApplication>,
  ): Observable<ProjectApplication | null> {
    return this.http.put<ProjectApplication>(APPLICATIONS_API, application).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null)),
    );
  }

  deleteApplication(id: number): Observable<boolean> {
    return this.http.delete(`${APPLICATIONS_API}/${id}`, { observe: 'response' }).pipe(
      map((res) => res.status >= 200 && res.status < 300),
      catchError(() => of(false)),
    );
  }

  getAllApplications(): Observable<ProjectApplication[]> {
    return this.http.get<ProjectApplication[]>(`${APPLICATIONS_API}/all`).pipe(
      timeout(REQUEST_TIMEOUT_MS)
    );
  }

  getApplicationById(id: number): Observable<ProjectApplication | null> {
    return this.http.get<ProjectApplication>(`${APPLICATIONS_API}/${id}`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of(null)),
    );
  }

  getApplicationsByProject(projectId: number): Observable<ProjectApplication[]> {
    return this.http.get<ProjectApplication[]>(`${APPLICATIONS_API}/project/${projectId}`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([])),
    );
  }

  getApplicationsByFreelance(freelanceId: number): Observable<ProjectApplication[]> {
    return this.http.get<ProjectApplication[]>(`${APPLICATIONS_API}/freelance/${freelanceId}`).pipe(
      timeout(REQUEST_TIMEOUT_MS),
      catchError(() => of([])),
    );
  }
}
