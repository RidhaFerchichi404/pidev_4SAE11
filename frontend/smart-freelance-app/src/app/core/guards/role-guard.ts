import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Role Guard - Protects routes based on user roles (ADMIN, CLIENT, FREELANCER)
 *
 * Usage in routes:
 * {
 *   path: 'admin',
 *   canActivate: [roleGuard],
 *   data: { roles: ['ADMIN'] }
 * }
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as string[] | undefined;

  // If no roles specified, allow access
  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  const userRole = auth.getUserRole();

  // Check if user has one of the required roles
  if (userRole && requiredRoles.includes(userRole)) {
    return true;
  }

  // Redirect based on current role
  if (userRole === 'ADMIN') {
    router.navigate(['/admin']);
  } else if (userRole === 'CLIENT' || userRole === 'FREELANCER') {
    router.navigate(['/dashboard']);
  } else {
    router.navigate(['/login']);
  }

  return false;
};
