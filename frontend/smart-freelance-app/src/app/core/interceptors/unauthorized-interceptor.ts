import { inject, Injector } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * When any API returns 401 (e.g. expired JWT), clear the token and redirect to login.
 *
 * Uses lazy injection via Injector to avoid a circular DI dependency:
 * AuthService constructor → HttpClient → interceptor → inject(AuthService) → NG0200.
 * By injecting Injector (always available) and resolving AuthService only on a 401,
 * we guarantee AuthService is fully constructed before we access it.
 */
export const unauthorizedInterceptor: HttpInterceptorFn = (req, next) => {
  const injector = inject(Injector);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err?.status === 401) {
        injector.get(AuthService).logout();
      }
      return throwError(() => err);
    })
  );
};
