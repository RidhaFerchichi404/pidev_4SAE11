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
      {
        path: 'my-projects',
        children: [
          // List / overview of all projects
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./pages/projects/list-projects/list-projects')
                .then(m => m.ListProjects),
            // title: 'My Projects'   ← optional – can be used by title service
          },

          // Create new project
          {
            path: 'add',
            loadComponent: () =>
              import('./pages/projects/add-project/add-project')
                .then(m => m.AddProject),
            title: 'Add New Project'
          },

          // View single project details
          {
            path: ':id/show',
            loadComponent: () =>
              import('./pages/projects/show-project/show-project')
                .then(m => m.ShowProject),
            title: 'Project Details'
          },

          // Edit existing project
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./pages/projects/update-project/update-project')
                .then(m => m.UpdateProject),
            title: 'Edit Project'
          },
        ]
      },
      { path: 'progress-updates', loadComponent: () => import('./pages/dashboard/progress-updates/progress-updates').then(m => m.ProgressUpdates) },
      { path: 'track-progress', loadComponent: () => import('./pages/dashboard/track-progress/track-progress').then(m => m.TrackProgress) },
      {
        path: 'my-offers',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/dashboard/my-offers/list-offers').then(m => m.ListOffers) },
          { path: 'add', loadComponent: () => import('./pages/dashboard/my-offers/add-offer').then(m => m.AddOffer) },
          { path: ':id/edit', loadComponent: () => import('./pages/dashboard/my-offers/edit-offer').then(m => m.EditOffer) },
          { path: ':id/show', loadComponent: () => import('./pages/dashboard/my-offers/show-offer').then(m => m.ShowOffer) },
        ]
      },
      {
        path: 'browse-offers',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/dashboard/browse-offers/browse-offers').then(m => m.BrowseOffers) },
          { path: ':id', loadComponent: () => import('./pages/dashboard/browse-offers/offer-detail').then(m => m.OfferDetail) },
        ]
      },
      { path: 'my-offer-applications', loadComponent: () => import('./pages/dashboard/my-offer-applications/my-offer-applications').then(m => m.MyOfferApplications) },
      {
        path: 'reviews',
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./pages/dashboard/reviews/my-reviews/my-reviews').then(m => m.MyReviews) },
          { path: 'about-me', loadComponent: () => import('./pages/dashboard/reviews/reviews-about-me/reviews-about-me').then(m => m.ReviewsAboutMe) },
          { path: 'add', loadComponent: () => import('./pages/dashboard/reviews/add-review/add-review').then(m => m.AddReview) },
          { path: ':id/edit', loadComponent: () => import('./pages/dashboard/reviews/edit-review/edit-review').then(m => m.EditReview) },
        ]
      },
     {
        path: 'browse-jobs',
        children: [
          // List / overview of all projects
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./pages/projects/list-projects/list-projects')
                .then(m => m.ListProjects),
            // title: 'My Projects'   ← optional – can be used by title service
          },

          // View single project details
          {
            path: ':id/show',
            loadComponent: () =>
              import('./pages/projects/show-project/show-project')
                .then(m => m.ShowProject),
            title: 'Project Details'
          },
        ]
      },

      {
        path: 'my-applications',
        children: [
          // List / overview of all applications
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./pages/projects-applications/list-application/list-application')
                .then(m => m.ListApplication),
            // title: 'My Applications'   ← optional – can be used by title service
          },

          // Create new application
          {
            path: 'add/:id',
            loadComponent: () =>
              import('./pages/projects-applications/add-application/add-application')
                .then(m => m.AddApplication),
            title: 'Add New Application'
          },

          // View single application details
          {
            path: ':id/show',
            loadComponent: () =>
              import('./pages/projects-applications/show-application/show-application')
                .then(m => m.ShowApplication),
            title: 'Application Details'
          },

          // Edit existing application
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./pages/projects-applications/update-application/update-application')
                .then(m => m.UpdateApplication),
            title: 'Edit Application'
          },
        ]
      },
      { path: 'my-portfolio', loadComponent: () => import('./pages/dashboard/portfolio-overview/portfolio-overview').then(m => m.PortfolioOverview) },
      { path: 'messages', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'notifications', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'profile', loadComponent: () => import('./pages/dashboard/profile/profile').then(m => m.Profile) },
      { path: 'settings', loadComponent: () => import('./pages/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome) },
      { path: 'my-contracts', loadComponent: () => import('./pages/dashboard/my-contracts/my-contracts').then(m => m.MyContracts) },
      { path: 'my-contracts/:id', loadComponent: () => import('./pages/dashboard/my-contracts/contract-detail/contract-detail').then(m => m.ContractDetail) },
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
      { path: 'users', loadComponent: () => import('./pages/admin/user-management/user-management').then(m => m.UserManagement) },
      { path: 'contracts', loadComponent: () => import('./pages/admin/admin-contracts/admin-contracts').then(m => m.AdminContracts) },

    
      { path: 'offers', loadComponent: () => import('./pages/admin/offer-management/offer-management').then(m => m.OfferManagement) },
      {
        path: 'projects',
        children: [
          // List / overview of all projects
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./pages/projects/list-projects/list-projects')
                .then(m => m.ListProjects),
            // title: 'My Projects'   ← optional – can be used by title service
          },

          // View single project details
          {
            path: ':id/show',
            loadComponent: () =>
              import('./pages/projects/show-project/show-project')
                .then(m => m.ShowProject),
            title: 'Project Details'
          },
        ]
      },

      { path: 'planning', loadComponent: () => import('./pages/admin/planning-management/planning-management').then(m => m.PlanningManagement) },
      { path: 'evaluations', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
      { path: 'reviews', loadComponent: () => import('./pages/admin/review-management/review-management').then(m => m.ReviewManagement) },
      { path: 'settings', loadComponent: () => import('./pages/admin/admin-dashboard/admin-dashboard').then(m => m.AdminDashboard) },
    ]
  },

  // Fallback
  { path: '**', redirectTo: '' },
];
