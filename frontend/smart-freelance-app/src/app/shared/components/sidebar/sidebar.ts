import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  label: string;
  route: string;
  icon: string; // icon key → looked up in iconPaths
}

interface NavSection {
  title: string;
  items: NavItem[];
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

  isExpanded = false;

  constructor(public auth: AuthService) {}

  toggle() { this.isExpanded = !this.isExpanded; }

  // ── SVG path data (24×24 Heroicons outline stroke) ────────

  readonly iconPaths: Record<string, string> = {
    dashboard:   'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
    users:       'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z',
    projects:    'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10',
    offers:      'M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z',
    contracts:   'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z',
    planning:    'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
    skills:      'M11 4a2 2 0 114 0v1a1 1 0 001 1h3a1 1 0 011 1v3a1 1 0 01-1 1h-1a2 2 0 100 4h1a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1v-1a2 2 0 10-4 0v1a1 1 0 01-1 1H7a1 1 0 01-1-1v-3a1 1 0 00-1-1H4a2 2 0 110-4h1a1 1 0 001-1V7a1 1 0 011-1h3a1 1 0 001-1V4z',
    'skill-stats':'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z',
    evaluations: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4',
    reviews:     'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z',
    settings:    'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065zM15 12a3 3 0 11-6 0 3 3 0 016 0z',
    search:      'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z',
    add:         'M12 4v16m8-8H4',
    folder:      'M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z',
    star:        'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z',
    chat:        'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z',
    bell:        'M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9',
    user:        'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z',
    chart:       'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z',
    portfolio:   'M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z',
  };

  // ── Admin nav sections ────────────────────────────────────

  get adminNavSections(): NavSection[] {
    return [
      {
        title: 'Overview',
        items: [
          { label: 'Dashboard', route: '/admin', icon: 'dashboard' },
        ]
      },
      {
        title: 'Management',
        items: [
          { label: 'Users',     route: '/admin/users',     icon: 'users'     },
          { label: 'Projects',  route: '/admin/projects',  icon: 'projects'  },
          { label: 'Offers',    route: '/admin/offers',    icon: 'offers'    },
          { label: 'Contracts', route: '/admin/contracts', icon: 'contracts' },
          { label: 'Planning',  route: '/admin/planning',  icon: 'planning'  },
        ]
      },
      {
        title: 'Skills',
        items: [
          { label: 'Skill Management', route: '/admin/skills',      icon: 'skills'      },
          { label: 'Skill Stats',      route: '/admin/skill-stats', icon: 'skill-stats' },
        ]
      },
      {
        title: 'Analytics',
        items: [
          { label: 'Evaluations', route: '/admin/evaluations', icon: 'evaluations' },
          { label: 'Reviews',     route: '/admin/reviews',     icon: 'reviews'     },
        ]
      },
      {
        title: 'System',
        items: [
          { label: 'Settings', route: '/admin/settings', icon: 'settings' },
        ]
      },
    ];
  }

  // ── Dashboard flat nav ────────────────────────────────────

  get navItems(): NavItem[] {
    const commonItems: NavItem[] = [
      { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
    ];

    if (this.auth.isClient()) {
      return [
        ...commonItems,
        { label: 'Browse Freelancers',    route: '/dashboard/browse-freelancers',    icon: 'search'    },
        { label: 'Browse Offers',         route: '/dashboard/browse-offers',         icon: 'offers'    },
        { label: 'My Offer Applications', route: '/dashboard/my-offer-applications', icon: 'contracts' },
        { label: 'Post a Job',            route: '/dashboard/post-job',              icon: 'add'       },
        { label: 'My Projects',           route: '/dashboard/my-projects',           icon: 'folder'    },
        { label: 'My Reviews',            route: '/dashboard/reviews',               icon: 'star'      },
        { label: 'Reviews about me',      route: '/dashboard/reviews/about-me',      icon: 'chat'      },
        { label: 'My Contracts',          route: '/dashboard/my-contracts',          icon: 'contracts' },
        { label: 'Track Progress',        route: '/dashboard/track-progress',        icon: 'chart'     },
        { label: 'Messages',              route: '/dashboard/messages',              icon: 'chat'      },
        { label: 'Notifications',         route: '/dashboard/notifications',         icon: 'bell'      },
        { label: 'Profile',               route: '/dashboard/profile',               icon: 'user'      },
        { label: 'Settings',              route: '/dashboard/settings',              icon: 'settings'  },
      ];
    }

    if (this.auth.isFreelancer()) {
      return [
        ...commonItems,
        { label: 'My Offers',          route: '/dashboard/my-offers',         icon: 'offers'     },
        { label: 'Browse Jobs',        route: '/dashboard/browse-jobs',       icon: 'search'     },
        { label: 'My Applications',    route: '/dashboard/my-applications',   icon: 'contracts'  },
        { label: 'My Reviews',         route: '/dashboard/reviews',           icon: 'star'       },
        { label: 'Reviews about me',   route: '/dashboard/reviews/about-me', icon: 'chat'       },
        { label: 'My Contracts',       route: '/dashboard/my-contracts',     icon: 'contracts'  },
        { label: 'My Progress Updates',route: '/dashboard/progress-updates', icon: 'chart'      },
        { label: 'My Portfolio',       route: '/dashboard/my-portfolio',     icon: 'portfolio'  },
        { label: 'Messages',           route: '/dashboard/messages',         icon: 'chat'       },
        { label: 'Notifications',      route: '/dashboard/notifications',    icon: 'bell'       },
        { label: 'Profile',            route: '/dashboard/profile',          icon: 'user'       },
        { label: 'Settings',           route: '/dashboard/settings',         icon: 'settings'   },
      ];
    }

    return commonItems;
  }
}
