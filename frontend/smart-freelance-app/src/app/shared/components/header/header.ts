import { Component, Input, OnInit, signal, HostListener, OnDestroy } from '@angular/core';
import { RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, switchMap } from 'rxjs/operators';
import { forkJoin, of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { NotificationService, NotificationItem } from '../../../core/services/notification.service';
import { ProfileViewService, ProfileViewItem } from '../../../core/services/profile-view.service';
import { Button } from '../button/button';
import { LiveSearch } from '../live-search/live-search.component';

const POLL_INTERVAL_MS = 15_000;
const TOAST_AUTO_DISMISS_MS = 5_000;

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterLink, RouterLinkActive, Button, LiveSearch],
  templateUrl: './header.html',
  styleUrl: './header.scss',
  standalone: true,
})
export class Header implements OnInit, OnDestroy {
  @Input() variant: 'public' | 'dashboard' | 'admin' = 'public';

  // Public mobile nav
  mobileMenuOpen = signal(false);
  // Dashboard mobile drawer
  dashboardMobileOpen = signal(false);
  // User dropdown
  userMenuOpen = signal(false);
  // Notifications dropdown
  notifDropdownOpen = signal(false);
  notificationList = signal<NotificationItem[]>([]);
  notifDropdownLoading = signal(false);
  // Active nav dropdown key: 'work' | 'growth' | 'talent' | 'manage' | null
  activeDropdown = signal<string | null>(null);
  // Scroll shadow
  isScrolled = signal(false);

  avatarUrl = signal<string | null>(null);
  notificationUnreadCount = signal<number>(0);
  profileViewCount = signal<number>(0);
  showToast = signal(false);
  toastMessage = signal('You have a new notification');
  deletingNotifId = signal<string | null>(null);

  // Views dropdown
  viewsDropdownOpen = signal(false);
  viewsDropdownLoading = signal(false);
  viewerList = signal<{ viewerId: number | null; viewedAt: string; name: string; avatarUrl: string | null }[]>([]);

  private routerSub?: ReturnType<typeof Router.prototype.events.subscribe>;
  private pollTimer?: ReturnType<typeof setInterval>;
  private toastTimer?: ReturnType<typeof setTimeout>;
  private lastUnreadCount = 0;

  constructor(
    public auth: AuthService,
    private userService: UserService,
    private notificationService: NotificationService,
    private profileViewSvc: ProfileViewService,
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
      this.refreshNotificationCount(true);
      if (this.auth.isFreelancer()) {
        const userId = this.auth.getUserId();
        if (userId != null) {
          this.profileViewSvc.getTotalCount(userId).subscribe(count => this.profileViewCount.set(count));
        }
      }
      this.routerSub = this.router.events
        .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
        .subscribe((e) => {
          if (e.urlAfterRedirects?.includes('/dashboard')) {
            this.refreshNotificationCount(true);
          }
        });
      // Poll for real-time unread count when on dashboard
      this.pollTimer = setInterval(() => this.refreshNotificationCount(false), POLL_INTERVAL_MS);
    }
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
    if (this.pollTimer) clearInterval(this.pollTimer);
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  private refreshNotificationCount(skipToast: boolean): void {
    const userId = this.auth.getUserId();
    if (userId == null) return;
    this.notificationService.getUnreadCount(userId).subscribe({
      next: (count) => {
        this.notificationUnreadCount.set(count);
        if (!skipToast && count > this.lastUnreadCount) {
          this.showToastSignal();
        }
        this.lastUnreadCount = count;
      },
      error: () => {
        this.notificationUnreadCount.set(0);
      },
    });
  }

  private showToastSignal(): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.showToast.set(true);
    this.toastTimer = setTimeout(() => {
      this.showToast.set(false);
      this.toastTimer = undefined;
    }, TOAST_AUTO_DISMISS_MS);
  }

  toggleNotifDropdown(): void {
    const open = !this.notifDropdownOpen();
    this.notifDropdownOpen.set(open);
    if (open) this.loadNotificationList();
    this.userMenuOpen.set(false);
    this.viewsDropdownOpen.set(false);
  }

  toggleViewsDropdown(): void {
    const open = !this.viewsDropdownOpen();
    this.viewsDropdownOpen.set(open);
    if (open) this.loadViewerList();
    this.notifDropdownOpen.set(false);
    this.userMenuOpen.set(false);
  }

  loadViewerList(): void {
    const userId = this.auth.getUserId();
    if (userId == null) return;
    this.viewsDropdownLoading.set(true);
    this.profileViewSvc.getRecentViewers(userId).pipe(
      switchMap((items: ProfileViewItem[]) => {
        const viewerIds = items.filter(v => v.viewerId != null).map(v => v.viewerId!);
        const unique = [...new Set(viewerIds)];
        if (unique.length === 0) {
          return of(items.map(v => ({ ...v, name: 'Anonymous', avatarUrl: null as string | null })));
        }
        const userCalls = unique.map(id => this.userService.getById(id));
        return forkJoin(userCalls).pipe(
          switchMap(users => {
            const nameMap = new Map<number, { name: string; avatarUrl: string | null }>();
            users.forEach((u, i) => {
              nameMap.set(unique[i], {
                name: u ? `${u.firstName} ${u.lastName}` : 'Unknown',
                avatarUrl: u?.avatarUrl?.trim() || null,
              });
            });
            return of(items.map(v => ({
              ...v,
              name: v.viewerId != null ? (nameMap.get(v.viewerId)?.name ?? 'Unknown') : 'Anonymous',
              avatarUrl: v.viewerId != null ? (nameMap.get(v.viewerId)?.avatarUrl ?? null) : null,
            })));
          })
        );
      })
    ).subscribe({
      next: list => { this.viewerList.set(list); this.viewsDropdownLoading.set(false); },
      error: () => { this.viewerList.set([]); this.viewsDropdownLoading.set(false); },
    });
  }

  loadNotificationList(): void {
    const userId = this.auth.getUserId();
    if (userId == null) return;
    this.notifDropdownLoading.set(true);
    this.notificationService.getByUserId(userId).subscribe({
      next: (list) => {
        this.notificationList.set(list.slice(0, 10));
        this.notifDropdownLoading.set(false);
      },
      error: () => {
        this.notificationList.set([]);
        this.notifDropdownLoading.set(false);
      },
    });
  }

  markNotifRead(n: NotificationItem): void {
    if (n.read) return;
    this.notificationService.markRead(n.id).subscribe(() => {
      this.notificationList.update((list) =>
        list.map((item) => (item.id === n.id ? { ...item, read: true } : item))
      );
      this.notificationUnreadCount.update((c) => Math.max(0, c - 1));
    });
  }

  deleteNotif(n: NotificationItem): void {
    this.deletingNotifId.set(n.id);
    this.notificationService.delete(n.id).subscribe({
      next: (ok) => {
        this.deletingNotifId.set(null);
        if (ok) {
          this.notificationList.update((list) => list.filter((item) => item.id !== n.id));
          if (!n.read) {
            this.notificationUnreadCount.update((c) => Math.max(0, c - 1));
          }
        }
      },
      error: () => this.deletingNotifId.set(null),
    });
  }

  /** Navigate to the relevant page for this notification and close dropdown. */
  goToNotification(n: NotificationItem): void {
    const { route, queryParams } = this.notificationService.getNotificationRoute(
      n,
      this.auth.isClient()
    );
    this.notifDropdownOpen.set(false);
    this.router.navigate([route], { queryParams });
    if (!n.read) this.markNotifRead(n);
  }

  goToViewer(viewerId: number | null): void {
    if (viewerId == null) return;
    this.viewsDropdownOpen.set(false);
    this.router.navigate(['/dashboard/freelancer-portfolio', viewerId]);
  }

  dismissToast(): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.showToast.set(false);
  }

  formatNotifTime(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return d.toLocaleDateString();
  }

  notifTypeLabel(type: string): string {
    if (type === 'PROGRESS_UPDATE') return 'Progress';
    if (type === 'PROGRESS_COMMENT') return 'Comment';
    return type || 'Notification';
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
    if (!target.closest('.notif-container')) {
      this.notifDropdownOpen.set(false);
    }
    if (!target.closest('.views-container')) {
      this.viewsDropdownOpen.set(false);
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
