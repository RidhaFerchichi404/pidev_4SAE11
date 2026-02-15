import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  label: string;
  route: string;
  icon: string;
}

@Component({
  selector: 'app-sidebar',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
  standalone: true,
})
export class Sidebar {
  @Input() variant: 'dashboard' | 'admin' = 'dashboard';

  constructor(public auth: AuthService) {}

  get navItems(): NavItem[] {
    if (this.variant === 'admin') {
      return [
        { label: 'Dashboard', route: '/admin', icon: '📊' },
        { label: 'Users', route: '/admin/users', icon: '👥' },
        { label: 'Offers', route: '/admin/offers', icon: '💼' },
        { label: 'Projects', route: '/admin/projects', icon: '🚀' },
        { label: 'Evaluations', route: '/admin/evaluations', icon: '📝' },
        { label: 'Reviews', route: '/admin/reviews', icon: '⭐' },
        { label: 'Settings', route: '/admin/settings', icon: '⚙️' },
      ];
    }

    // Dashboard (CLIENT/FREELANCER)
    const commonItems: NavItem[] = [
      { label: 'Dashboard', route: '/dashboard', icon: '🏠' },
    ];

    if (this.auth.isClient()) {
      return [
        ...commonItems,
        { label: 'Browse Freelancers', route: '/dashboard/browse-freelancers', icon: '🔍' },
        { label: 'Post a Job', route: '/dashboard/post-job', icon: '➕' },
        { label: 'My Projects', route: '/dashboard/my-projects', icon: '📁' },
        { label: 'Track Progress', route: '/dashboard/track-progress', icon: '📊' },
        { label: 'Messages', route: '/dashboard/messages', icon: '💬' },
        { label: 'Notifications', route: '/dashboard/notifications', icon: '🔔' },
        { label: 'Profile', route: '/dashboard/profile', icon: '👤' },
        { label: 'Settings', route: '/dashboard/settings', icon: '⚙️' },
      ];
    }

    if (this.auth.isFreelancer()) {
      return [
        ...commonItems,
        { label: 'Browse Jobs', route: '/dashboard/browse-jobs', icon: '🔍' },
        { label: 'My Applications', route: '/dashboard/my-applications', icon: '📋' },
        { label: 'My Progress Updates', route: '/dashboard/progress-updates', icon: '📊' },
        { label: 'My Portfolio', route: '/dashboard/my-portfolio', icon: '🎨' },
        { label: 'Messages', route: '/dashboard/messages', icon: '💬' },
        { label: 'Notifications', route: '/dashboard/notifications', icon: '🔔' },
        { label: 'Profile', route: '/dashboard/profile', icon: '👤' },
        { label: 'Settings', route: '/dashboard/settings', icon: '⚙️' },
      ];
    }

    return commonItems;
  }
}
