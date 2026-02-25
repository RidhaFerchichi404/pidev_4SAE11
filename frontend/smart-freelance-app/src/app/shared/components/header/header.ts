import { Component, Input, OnInit, signal, HostListener } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { Button } from '../button/button';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterLink, RouterLinkActive, Button],
  templateUrl: './header.html',
  styleUrl: './header.scss',
  standalone: true,
})
export class Header implements OnInit {
  @Input() variant: 'public' | 'dashboard' | 'admin' = 'public';

  // Public mobile nav
  mobileMenuOpen = signal(false);
  // Dashboard mobile drawer
  dashboardMobileOpen = signal(false);
  // User dropdown
  userMenuOpen = signal(false);
  // Active nav dropdown key: 'work' | 'growth' | 'talent' | 'manage' | null
  activeDropdown = signal<string | null>(null);
  // Scroll shadow
  isScrolled = signal(false);

  avatarUrl = signal<string | null>(null);

  constructor(
    public auth: AuthService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      const email = this.auth.getPreferredUsername();
      if (email) {
        this.userService.getByEmail(email).subscribe((user) => {
          const url = user?.avatarUrl?.trim() || null;
          setTimeout(() => this.avatarUrl.set(url), 0);
        });
      }
    }
  }

  get logoRoute(): string {
    if (!this.auth.isLoggedIn()) return '/';
    return this.auth.isAdmin() ? '/admin' : '/dashboard';
  }

  // ── Toggles ────────────────────────────────────────────────

  toggleMobileMenu(): void { this.mobileMenuOpen.update(v => !v); }
  toggleDashboardMobile(): void { this.dashboardMobileOpen.update(v => !v); }
  toggleUserMenu(): void { this.userMenuOpen.update(v => !v); }

  openDropdown(name: string): void { this.activeDropdown.set(name); }
  closeDropdown(): void { this.activeDropdown.set(null); }
  toggleDropdown(name: string): void {
    this.activeDropdown.update(current => current === name ? null : name);
  }

  // ── Search ─────────────────────────────────────────────────

  handleSearch(event: Event, query: string): void {
    event.preventDefault();
    const q = query.trim();
    if (q) {
      this.router.navigate(['/dashboard/search'], { queryParams: { q } });
    }
  }

  // ── Auth ───────────────────────────────────────────────────

  logout(): void {
    this.auth.logout();
    this.userMenuOpen.set(false);
  }

  onAvatarError(): void { this.avatarUrl.set(null); }

  // ── Scroll shadow ──────────────────────────────────────────

  @HostListener('window:scroll')
  onScroll(): void {
    this.isScrolled.set(window.scrollY > 8);
  }

  // ── Outside-click close ────────────────────────────────────

  @HostListener('document:click', ['$event'])
  onDocumentClick(e: MouseEvent): void {
    const target = e.target as HTMLElement;

    if (!target.closest('.user-menu-container')) {
      this.userMenuOpen.set(false);
    }
    if (!target.closest('.nav-group')) {
      this.activeDropdown.set(null);
    }
    // Close dashboard mobile drawer when clicking outside the header
    if (!target.closest('.header')) {
      this.dashboardMobileOpen.set(false);
    }
  }
}
