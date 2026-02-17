import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';



@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  private apiUrl = `${environment.apiProject}/projects`;

  constructor(private http: HttpClient) {}

  getAllProjects(): Observable<any> {
    return this.http.get(this.apiUrl+"/list");
  }

  getProjectById(id: string | number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}`);
  }

  createProject(project: any): Observable<any> {
    return this.http.post(this.apiUrl+"/add", project);
  }

  updateProject(project: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/update`, project);
  }

  deleteProject(id: string | number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
