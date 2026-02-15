import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * When any API returns 401 (e.g. expired JWT), clear the token and redirect to login
 * so the user gets a fresh session instead of seeing "Loading..." or generic errors.
 */
export const unauthorizedInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err?.status === 401) {
        auth.logout();
      }
      return throwError(() => err);
    })
  );
};
