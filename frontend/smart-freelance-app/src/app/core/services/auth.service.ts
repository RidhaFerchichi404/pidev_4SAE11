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
  phone?: string;
  avatarUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = `${environment.apiGatewayUrl}/${environment.authApiPrefix}`;
  private tokenSignal = signal<string | null>(this.getStoredToken());

  isLoggedIn = computed(() => !!this.tokenSignal());
  isAdmin = computed(() => this.getUserRole() === 'ADMIN');
  isClient = computed(() => this.getUserRole() === 'CLIENT');
  isFreelancer = computed(() => this.getUserRole() === 'FREELANCER');

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  private getStoredToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  /** Success: LoginResponse with access_token. Failure: { error: string } with a user-friendly message. */
  login(email: string, password: string): Observable<LoginResponse | { error: string }> {
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
        catchError((err) => of({ error: this.mapLoginError(err) }))
      );
  }

  /** Map HTTP/backend errors to user-friendly login messages. */
  private mapLoginError(err: { status?: number; error?: { error_description?: string; error?: string }; message?: string }): string {
    const status = err?.status;
    const body = err?.error;
    const desc = (typeof body === 'object' && body?.error_description) ? String(body.error_description).toLowerCase() : '';
    const msg = (typeof body === 'object' && body?.error) ? String(body.error).toLowerCase() : '';

    if (status === 401) {
      if (desc.includes('invalid') && (desc.includes('user') || desc.includes('credential'))) {
        return 'Invalid email or password. This account may not exist or the password is incorrect.';
      }
      return 'Invalid email or password. Please check your credentials.';
    }
    if (status === 404 || msg.includes('not found')) {
      return 'This account does not exist. Please sign up first.';
    }
    if (status === 400) {
      return 'Invalid request. Please check your email and password format.';
    }
    if (status === 0 || status === undefined) {
      return 'Cannot reach the server. Check your connection and try again.';
    }
    if (status && status >= 500) {
      return 'Authentication service is temporarily unavailable. Please try again later.';
    }
    return err?.message || 'Login failed. Please try again.';
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

  /** Create a new user (admin "Add user"). Uses the public register endpoint. */
  adminCreateUser(request: RegisterRequest): Observable<{ message?: string; keycloakUserId?: string } | { error: string }> {
    const url = `${this.baseUrl}/register`;
    return this.http.post<{ message: string; keycloakUserId: string }>(url, request).pipe(
      catchError((err) => {
        const status = err?.status;
        const backendMessage = err?.error?.error ?? err?.error?.message;
        const raw = typeof backendMessage === 'string' ? backendMessage : err?.message || '';
        const message =
          status === 409 || (raw && raw.toLowerCase().includes('already exists'))
            ? 'A user with this email already exists. Use a different email or edit the existing user.'
            : toUserFriendlyAuthError(raw || 'Failed to create user.');
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

  /**
   * Decode JWT token to extract user roles.
   * Keycloak stores roles in the 'realm_access.roles' claim.
   */
  private decodeToken(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch {
      console.error('[AuthService] Failed to decode token');
      return null;
    }
  }

  /**
   * Extract user role from JWT token.
   * Priority: ADMIN > CLIENT > FREELANCER
   * @returns 'ADMIN' | 'CLIENT' | 'FREELANCER' | null
   */
  getUserRole(): string | null {
    const token = this.getToken();
    if (!token) return null;

    const decoded = this.decodeToken(token);
    const roles = decoded?.realm_access?.roles || [];

    // Priority order
    if (roles.includes('ADMIN')) return 'ADMIN';
    if (roles.includes('CLIENT')) return 'CLIENT';
    if (roles.includes('FREELANCER')) return 'FREELANCER';

    return null;
  }

  /**
   * Display name for the current user from JWT (given_name + family_name, or name, or preferred_username).
   */
  getDisplayName(): string {
    const token = this.getToken();
    if (!token) return 'Me';

    const decoded = this.decodeToken(token);
    if (!decoded) return 'Me';

    const given = decoded.given_name;
    const family = decoded.family_name;
    if (given && family) return `${given} ${family}`.trim();
    if (given) return given;
    if (family) return family;
    if (decoded.name) return decoded.name;
    if (decoded.preferred_username) return decoded.preferred_username;

    return 'Me';
  }

  /** Email (preferred_username) of the current user, or null if not logged in. */
  getPreferredUsername(): string | null {
    const token = this.getToken();
    if (!token) return null;
    const decoded = this.decodeToken(token);
    return decoded?.preferred_username ?? null;
  }
}
