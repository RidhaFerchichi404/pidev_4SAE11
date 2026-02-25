import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of, switchMap, map } from 'rxjs';
import { environment } from '../../../environments/environment';

const TOKEN_KEY         = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const USER_ID_KEY       = 'user_id';

/** 15 minutes of inactivity → auto-logout */
const INACTIVITY_MS = 15 * 60 * 1000;

/** Map backend/Keycloak errors to a short message for the UI. */
function toUserFriendlyAuthError(raw: string): string {
  const s = raw.toLowerCase();
  if (s.includes('connection refused') || s.includes('econnrefused')) {
    return 'Authentication server is unavailable. Please ensure Keycloak is running.';
  }
  if (s.includes('user with email already exists')) {
    return 'An account with this email already exists. Try signing in or use another email.';
  }
  if (s.includes('keycloak rejected admin') || s.includes('keycloak admin')) {
    return 'Signup is misconfigured on the server (Keycloak admin credentials). Contact the administrator or check the Keycloak auth service configuration.';
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

interface UserProfile {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = `${environment.apiGatewayUrl}/${environment.authApiPrefix}`;
  private readonly userUrl = `${environment.apiGatewayUrl}/user/api/users`;

  private tokenSignal = signal<string | null>(this.getStoredToken());
  public userIdSignal = signal<number | null>(this.getStoredUserId());

  isLoggedIn   = computed(() => !!this.tokenSignal());
  isAdmin      = computed(() => this.getUserRole() === 'ADMIN');
  isClient     = computed(() => this.getUserRole() === 'CLIENT');
  isFreelancer = computed(() => this.getUserRole() === 'FREELANCER');

  // Timers
  private inactivityTimer: ReturnType<typeof setTimeout> | null = null;
  private refreshTimer: ReturnType<typeof setTimeout> | null = null;
  private activityListenersBound = false;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    // Defer profile fetch to avoid circular dependency (auth interceptor injects AuthService)
    if (this.tokenSignal() && !this.userIdSignal()) {
      setTimeout(() => this.fetchUserProfile().subscribe(), 0);
    }
    // Resume timers if already logged in (e.g. page refresh)
    if (this.tokenSignal()) {
      this.scheduleTokenRefresh();
      this.startInactivityTimer();
      this.bindActivityListeners();
    }
  }

  // ── Storage helpers ────────────────────────────────────────

  private getStoredToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private getStoredRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  private getStoredUserId(): number | null {
    const stored = localStorage.getItem(USER_ID_KEY);
    return stored ? Number(stored) : null;
  }

  // ── User profile ───────────────────────────────────────────

  fetchUserProfile(): Observable<UserProfile | null> {
    const token = this.tokenSignal();
    if (!token) return of(null);

    const decoded = this.decodeToken(token);
    const userEmail = decoded?.email;

    if (!userEmail) {
      console.warn('[AuthService] No email found in token, cannot fetch user ID');
      return of(null);
    }

    return this.http.get<UserProfile>(`${this.userUrl}/email/${userEmail}`).pipe(
      tap((user) => {
        if (user?.id) {
          localStorage.setItem(USER_ID_KEY, String(user.id));
          this.userIdSignal.set(user.id);
          console.log('[AuthService] Stored numeric user ID:', user.id);
        }
      }),
      catchError((err) => {
        console.error('[AuthService] Failed to fetch user profile:', err);
        return of(null);
      })
    );
  }

  // ── Login ──────────────────────────────────────────────────

  /** Success: LoginResponse with access_token. Failure: { error: string }. */
  login(email: string, password: string): Observable<LoginResponse | { error: string }> {
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/token`, { username: email, password } as LoginRequest)
      .pipe(
        tap((res) => {
          if (res?.access_token) {
            localStorage.setItem(TOKEN_KEY, res.access_token);
            this.tokenSignal.set(res.access_token);
          }
          if (res?.refresh_token) {
            localStorage.setItem(REFRESH_TOKEN_KEY, res.refresh_token);
          }
        }),
        switchMap((res) => {
          if (res?.access_token) {
            return this.fetchUserProfile().pipe(map(() => res));
          }
          return of(res);
        }),
        tap((res) => {
          if ((res as LoginResponse)?.access_token) {
            this.scheduleTokenRefresh();
            this.startInactivityTimer();
            this.bindActivityListeners();
          }
        }),
        catchError((err) => of({ error: this.mapLoginError(err) }))
      );
  }

  // ── Token refresh ──────────────────────────────────────────

  /** Call the backend refresh endpoint and update stored tokens. */
  refreshToken(): Observable<LoginResponse | { error: string }> {
    const rt = this.getStoredRefreshToken();
    if (!rt) return of({ error: 'No refresh token available' });

    return this.http
      .post<LoginResponse>(`${this.baseUrl}/refresh`, { refresh_token: rt })
      .pipe(
        tap((res) => {
          if (res?.access_token) {
            localStorage.setItem(TOKEN_KEY, res.access_token);
            this.tokenSignal.set(res.access_token);
          }
          if (res?.refresh_token) {
            localStorage.setItem(REFRESH_TOKEN_KEY, res.refresh_token);
          }
          this.scheduleTokenRefresh();
        }),
        catchError(() => {
          console.warn('[AuthService] Token refresh failed — logging out');
          this.logout();
          return of({ error: 'Session expired. Please log in again.' });
        })
      );
  }

  /**
   * Schedule an automatic token refresh 60 seconds before the JWT expires.
   * Reads the `exp` claim from the current access token.
   */
  private scheduleTokenRefresh(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }

    const token = this.tokenSignal();
    if (!token) return;

    const decoded = this.decodeToken(token);
    if (!decoded?.exp) return;

    const expiresAtMs = decoded.exp * 1000;
    const nowMs       = Date.now();
    const refreshInMs = expiresAtMs - nowMs - 60_000; // 1 min before expiry

    if (refreshInMs <= 0) {
      // Already expired or about to expire — refresh immediately
      this.refreshToken().subscribe();
      return;
    }

    console.log(`[AuthService] Scheduling token refresh in ${Math.round(refreshInMs / 1000)}s`);
    this.refreshTimer = setTimeout(() => this.refreshToken().subscribe(), refreshInMs);
  }

  // ── Inactivity timeout (15 min) ────────────────────────────

  private startInactivityTimer(): void {
    if (this.inactivityTimer) clearTimeout(this.inactivityTimer);
    this.inactivityTimer = setTimeout(() => {
      console.warn('[AuthService] Inactivity timeout — logging out');
      this.logout();
    }, INACTIVITY_MS);
  }

  private resetInactivityTimer(): void {
    this.startInactivityTimer();
  }

  /** Bind DOM activity events once to reset the inactivity timer on any user interaction. */
  private bindActivityListeners(): void {
    if (this.activityListenersBound) return;
    this.activityListenersBound = true;

    const events = ['mousemove', 'mousedown', 'keydown', 'touchstart', 'scroll', 'click'];
    const reset = () => this.resetInactivityTimer();
    events.forEach(event =>
      document.addEventListener(event, reset, { passive: true })
    );
  }

  // ── Logout ─────────────────────────────────────────────────

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_ID_KEY);
    this.tokenSignal.set(null);
    this.userIdSignal.set(null);

    if (this.inactivityTimer) { clearTimeout(this.inactivityTimer); this.inactivityTimer = null; }
    if (this.refreshTimer)    { clearTimeout(this.refreshTimer);    this.refreshTimer    = null; }

    this.router.navigate(['/login']);
  }

  // ── Token accessors ────────────────────────────────────────

  getToken(): string | null {
    return this.tokenSignal();
  }

  getUserId(): number | null {
    return this.userIdSignal();
  }

  // ── JWT helpers ────────────────────────────────────────────

  private decodeToken(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch {
      console.error('[AuthService] Failed to decode token');
      return null;
    }
  }

  getUserRole(): string | null {
    const token = this.getToken();
    if (!token) return null;
    const decoded = this.decodeToken(token);
    const roles = decoded?.realm_access?.roles || [];
    if (roles.includes('ADMIN'))      return 'ADMIN';
    if (roles.includes('CLIENT'))     return 'CLIENT';
    if (roles.includes('FREELANCER')) return 'FREELANCER';
    return null;
  }

  getDisplayName(): string {
    const token = this.getToken();
    if (!token) return 'Me';
    const decoded = this.decodeToken(token);
    if (!decoded) return 'Me';
    const given  = decoded.given_name;
    const family = decoded.family_name;
    if (given && family) return `${given} ${family}`.trim();
    if (given)  return given;
    if (family) return family;
    if (decoded.name) return decoded.name;
    if (decoded.preferred_username) return decoded.preferred_username;
    return 'Me';
  }

  getPreferredUsername(): string | null {
    const token = this.getToken();
    if (!token) return null;
    const decoded = this.decodeToken(token);
    return decoded?.preferred_username ?? null;
  }

  // ── Login error mapping ────────────────────────────────────

  private mapLoginError(err: {
    status?: number;
    error?: { error_description?: string; error?: string };
    message?: string;
  }): string {
    const status = err?.status;
    const body   = err?.error;
    const desc   = (typeof body === 'object' && body?.error_description)
      ? String(body.error_description).toLowerCase() : '';
    const msg    = (typeof body === 'object' && body?.error)
      ? String(body.error).toLowerCase() : '';

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

  /** Map backend/Keycloak errors to a short message. */
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
            status: err?.status, statusText: err?.statusText,
            error: err?.error, message,
          });
          return of({ error: message });
        })
      );
  }

  /** Create a new user (admin "Add user"). */
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
}
