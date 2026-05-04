import { inject, Injector } from '@angular/core';
import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, finalize, map, Observable, shareReplay, switchMap, throwError } from 'rxjs';
import { AuthService, readAccessTokenFromBody } from '../services/auth.service';

/** Auth endpoints where 401 means "wrong credentials" — we should NOT logout/redirect. */
const AUTH_ENDPOINTS = ['/token', '/refresh'];

/** Non-auth calls where 401 must not trigger logout (e.g. optional probe; gateway may differ from /task/). */
const NO_LOGOUT_ON_401_SUBSTRINGS = [
  'aimodel/api/ai/status',
  '/subcontracting/api/subcontracts'
];

const TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const RETRY_ON_401 = new HttpContextToken<boolean>(() => false);

let refreshInFlight$: Observable<string | null> | null = null;

function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
}

function getStoredRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || sessionStorage.getItem(REFRESH_TOKEN_KEY);
}

function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1] ?? ''));
    const exp = Number(payload?.exp);
    if (!Number.isFinite(exp) || exp <= 0) return false;
    return Date.now() >= exp * 1000;
  } catch {
    // If token cannot be decoded, do not force logout from interceptor.
    return false;
  }
}

/**
 * When any API returns 401 (e.g. expired JWT), clear the token and redirect to login.
 * Skips logout for auth endpoints (login, refresh) so the login form can show the error.
 *
 * Uses lazy injection via Injector to avoid a circular DI dependency:
 * AuthService constructor → HttpClient → interceptor → inject(AuthService) → NG0200.
 */
export const unauthorizedInterceptor: HttpInterceptorFn = (req, next) => {
  const injector = inject(Injector);
  const isAuthRequest = AUTH_ENDPOINTS.some((ep) => req.url.includes(ep));
  const skipLogoutForUrl = NO_LOGOUT_ON_401_SUBSTRINGS.some((s) => req.url.includes(s));
  const alreadyRetried = req.context.get(RETRY_ON_401);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err?.status === 401 && !isAuthRequest && !alreadyRetried) {
        const refreshToken = getStoredRefreshToken();
        if (refreshToken) {
          const auth = injector.get(AuthService);

          refreshInFlight$ ??= auth.refreshToken().pipe(
              map((res) => readAccessTokenFromBody(res)),
              shareReplay(1),
              finalize(() => {
                refreshInFlight$ = null;
              })
            );

          return refreshInFlight$.pipe(
            switchMap((newToken) => {
              if (!newToken) {
                auth.logout();
                return throwError(() => err);
              }

              const userId =
                localStorage.getItem('user_id') || sessionStorage.getItem('user_id');
              const headers: Record<string, string> = {
                Authorization: `Bearer ${newToken}`
              };
              if (userId) headers['X-User-Id'] = userId;

              return next(
                req.clone({
                  context: req.context.set(RETRY_ON_401, true),
                  setHeaders: headers
                })
              );
            }),
            catchError(() => {
              auth.logout();
              return throwError(() => err);
            })
          );
        }
      }

      if (err?.status === 401 && !isAuthRequest && !skipLogoutForUrl) {
        const token = getStoredToken();
        const shouldLogout = !token || isTokenExpired(token);
        if (shouldLogout) {
          injector.get(AuthService).logout();
        }
      }
      return throwError(() => err);
    })
  );
};
