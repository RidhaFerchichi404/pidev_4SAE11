import { Component, signal, HostListener } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  label: string;
  route: string;
  icon: string;
}

@Component({
  selector: 'app-dashboard-navbar',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './dashboard-navbar.html',
  styleUrl: './dashboard-navbar.scss',
  standalone: true,
})
export class DashboardNavbar {
  mobileOpen = signal(false);

  constructor(public auth: AuthService) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(e: MouseEvent) {
    if (!(e.target as HTMLElement).closest('.dashboard-navbar')) {
      this.mobileOpen.set(false);
    }
  }

  toggle() {
    this.mobileOpen.update(v => !v);
  }

  get navItems(): NavItem[] {
    if (this.auth.isClient()) {
      return [
        { label: 'Home',               route: '/dashboard',                      icon: '🏠' },
        { label: 'Freelancers',        route: '/dashboard/browse-freelancers',   icon: '🔍' },
        { label: 'Browse Offers',      route: '/dashboard/browse-offers',        icon: '💼' },
        { label: 'My Applications',    route: '/dashboard/my-offer-applications',icon: '📝' },
        { label: 'Post a Job',         route: '/dashboard/post-job',             icon: '➕' },
        { label: 'My Projects',        route: '/dashboard/my-projects',          icon: '📁' },
        { label: 'My Contracts',       route: '/dashboard/my-contracts',         icon: '📋' },
        { label: 'Track Progress',     route: '/dashboard/track-progress',       icon: '📊' },
        { label: 'Reviews',            route: '/dashboard/reviews',              icon: '⭐' },
        { label: 'Messages',           route: '/dashboard/messages',             icon: '💬' },
        { label: 'Notifications',      route: '/dashboard/notifications',        icon: '🔔' },
        { label: 'Profile',            route: '/dashboard/profile',              icon: '👤' },
        { label: 'Settings',           route: '/dashboard/settings',             icon: '⚙️' },
      ];
    }

    if (this.auth.isFreelancer()) {
      return [
        { label: 'Home',               route: '/dashboard',                      icon: '🏠' },
        { label: 'My Offers',          route: '/dashboard/my-offers',            icon: '💼' },
        { label: 'Browse Jobs',        route: '/dashboard/browse-jobs',          icon: '🔍' },
        { label: 'My Applications',    route: '/dashboard/my-applications',      icon: '📋' },
        { label: 'My Contracts',       route: '/dashboard/my-contracts',         icon: '📋' },
        { label: 'My Progress',        route: '/dashboard/progress-updates',     icon: '📊' },
        { label: 'My Portfolio',       route: '/dashboard/my-portfolio',         icon: '🎨' },
        { label: 'Reviews',            route: '/dashboard/reviews',              icon: '⭐' },
        { label: 'Messages',           route: '/dashboard/messages',             icon: '💬' },
        { label: 'Notifications',      route: '/dashboard/notifications',        icon: '🔔' },
        { label: 'Profile',            route: '/dashboard/profile',              icon: '👤' },
        { label: 'Settings',           route: '/dashboard/settings',             icon: '⚙️' },
      ];
    }

    return [{ label: 'Home', route: '/dashboard', icon: '🏠' }];
  }
}
