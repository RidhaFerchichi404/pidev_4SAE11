import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Auth Interceptor - Automatically adds JWT token to HTTP requests
 *
 * Reads the token directly from localStorage to avoid a circular DI
 * dependency (AuthService → HttpClient → authInterceptor → AuthService).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('access_token');

  if (token) {
    return next(req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    }));
  }

  return next(req);
};
