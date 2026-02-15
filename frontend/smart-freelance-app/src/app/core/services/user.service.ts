import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export type UserRole = 'CLIENT' | 'FREELANCER' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  phone?: string;
  avatarUrl?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserRequest {
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  phone?: string;
  avatarUrl?: string;
  isActive?: boolean;
}

const USER_API = `${environment.apiGatewayUrl}/user/api/users`;

export interface AvatarUploadResponse {
  avatarUrl: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private http: HttpClient) {}

  /** Upload an image file as avatar. Returns the URL to use as avatarUrl. */
  uploadAvatar(file: File): Observable<string | null> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<AvatarUploadResponse>(`${USER_API}/avatar`, formData).pipe(
      map((res) => res?.avatarUrl ?? null),
      catchError(() => of(null))
    );
  }

  getAll(): Observable<User[]> {
    return this.http.get<User[]>(USER_API);
  }

  getById(id: number): Observable<User | null> {
    return this.http.get<User>(`${USER_API}/${id}`).pipe(
      catchError(() => of(null))
    );
  }

  getByEmail(email: string): Observable<User | null> {
    const encoded = encodeURIComponent(email);
    return this.http.get<User>(`${USER_API}/email/${encoded}`).pipe(
      catchError(() => of(null))
    );
  }

  create(request: UserRequest): Observable<User | null> {
    return this.http.post<User>(USER_API, request).pipe(
      catchError(() => of(null))
    );
  }

  update(id: number, request: Partial<UserRequest>): Observable<User | null> {
    return this.http.put<User>(`${USER_API}/${id}`, request).pipe(
      catchError(() => of(null))
    );
  }

  delete(id: number): Observable<boolean> {
    return this.http.delete(`${USER_API}/${id}`, { observe: 'response' }).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }
}
