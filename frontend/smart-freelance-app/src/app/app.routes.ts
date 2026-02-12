import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role-guard';

export const routes: Routes = [
  // Public routes with layout
  {
    path: '',
    loadComponent: () => import('./layouts/public-layout/public-layout').then(m => m.PublicLayout),
    children: [
      { path: '', loadComponent: () => import('./pages/public/home/home').then(m => m.Home) },
      { path: 'how-it-works', loadComponent: () => import('./pages/public/how-it-works/how-it-works').then(m => m.HowItWorks) },
      { path: 'about', loadComponent: () => import('./pages/public/about/about').then(m => m.About) },
    ]
  },

  // Auth pages (no layout)
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'signup', loadComponent: () => import('./pages/signup/signup.component').then(m => m.SignupComponent) },

  // Dashboard routes (protected, CLIENT/FREELANCER only)
  {
    path: 'dashboard',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENT', 'FREELANCER'] },
    loadComponent: () => import('./layouts/dashboard-layout/dashboard-layout').then(m => m.DashboardLayout),
    children: [
      { path: '', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      // Placeholder routes for future implementation
      { path: 'browse-freelancers', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'post-job', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'my-projects', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'browse-jobs', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'my-applications', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'my-portfolio', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'messages', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'notifications', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'profile', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'settings', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
    ]
  },

  // Admin routes (protected, ADMIN only)
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () => import('./layouts/admin-layout/admin-layout').then(m => m.AdminLayout),
    children: [
      { path: '', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
      // Placeholder routes for future implementation
      { path: 'users', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
      { path: 'offers', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
      { path: 'projects', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
      { path: 'evaluations', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
      { path: 'reviews', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
      { path: 'settings', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
    ]
  },

  // Fallback
  { path: '**', redirectTo: '' },
];
