import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

const PROJECT_API = `${environment.apiGatewayUrl}/project/projects`;
const APPLICATIONS_API = `${environment.apiGatewayUrl}/project/applications`;

export interface Project {
  id: number;
  clientId: number;
  title: string;
  description: string;
  budget?: number;
  deadline?: string;
  status?: string;
  category?: string;
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
  constructor(private http: HttpClient) {}

  getById(id: number): Observable<Project | null> {
    return this.http.get<Project>(`${PROJECT_API}/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  getByClientId(clientId: number): Observable<Project[]> {
    return this.http.get<Project[]>(`${PROJECT_API}/client/${clientId}`).pipe(
      catchError(() => of([]))
    );
  }

  getApplicationsByFreelancer(freelancerId: number): Observable<ProjectApplication[]> {
    return this.http.get<ProjectApplication[]>(`${APPLICATIONS_API}/freelance/${freelancerId}`).pipe(
      catchError(() => of([]))
    );
  }
}
