import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'signup', loadComponent: () => import('./pages/signup/signup.component').then(m => m.SignupComponent) },
  {
    path: 'success',
    loadComponent: () => import('./pages/success/success.component').then(m => m.SuccessComponent),
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: 'login' },
];
