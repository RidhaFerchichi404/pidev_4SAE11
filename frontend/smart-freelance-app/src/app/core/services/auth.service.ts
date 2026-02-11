import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'access_token';

/** Map backend/Keycloak errors to a short message for the UI. */
function toUserFriendlyAuthError(raw: string): string {
  const s = raw.toLowerCase();
  if (s.includes('connection refused') || s.includes('econnrefused')) {
    return 'Authentication server is unavailable. Please ensure Keycloak is running on port 8421.';
  }
  if (s.includes('user with email already exists')) {
    return 'An account with this email already exists. Try signing in or use another email.';
  }
  return raw.length > 120 ? raw.slice(0, 120) + '…' : raw;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  refresh_token?: string;
  token_type?: string;
  expires_in?: number;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = `${environment.apiGatewayUrl}/${environment.authApiPrefix}`;
  private tokenSignal = signal<string | null>(this.getStoredToken());

  isLoggedIn = computed(() => !!this.tokenSignal());

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  private getStoredToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  login(email: string, password: string): Observable<LoginResponse | null> {
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/token`, {
        username: email,
        password,
      } as LoginRequest)
      .pipe(
        tap((res) => {
          if (res?.access_token) {
            localStorage.setItem(TOKEN_KEY, res.access_token);
            this.tokenSignal.set(res.access_token);
          }
        }),
        catchError(() => of(null))
      );
  }

  /** On success returns { message, keycloakUserId }; on HTTP error returns { error: string } with backend message when available. */
  register(request: RegisterRequest): Observable<{ message?: string; keycloakUserId?: string } | { error: string }> {
    const url = `${this.baseUrl}/register`;
    console.log('[AuthService] Sign up: sending POST to', url, '| body (no password):', { ...request, password: '***' });
    return this.http
      .post<{ message: string; keycloakUserId: string }>(url, request)
      .pipe(
        tap((res) => console.log('[AuthService] Sign up: success', res)),
        catchError((err) => {
          const backendMessage = err?.error?.error ?? err?.error?.message;
          const raw = typeof backendMessage === 'string'
            ? backendMessage
            : err?.message || 'Registration failed. Please try again.';
          const message = toUserFriendlyAuthError(raw);
          console.error('[AuthService] Sign up: request failed', {
            status: err?.status,
            statusText: err?.statusText,
            error: err?.error,
            message,
          });
          return of({ error: message });
        })
      );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.tokenSignal.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.tokenSignal();
  }
}
