import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { AuthService } from '../services/auth.service';

/**
 * Auth Interceptor - Automatically adds JWT token to HTTP requests
 *
 * Adds 'Authorization: Bearer <token>' header to all outgoing requests
 * if the user is logged in.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  // If token exists, clone request and add Authorization header
  if (token) {
    const clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(clonedReq);
  }

  // No token, pass original request
  return next(req);
};
