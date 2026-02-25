import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { ProjectService, Project } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { HttpErrorResponse } from '@angular/common/http';
import { interval, Subscription, forkJoin, of } from 'rxjs';
import { catchError, map, filter } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';


const AUTO_REFRESH_INTERVAL_MS = 60_000;

/** Normalize API response to Project[] (handles raw array or wrapped { data/content }). */
function toProjectList(res: unknown): Project[] {
  if (Array.isArray(res)) return res as Project[];
  if (res && typeof res === 'object') {
    const o = res as Record<string, unknown>;
    if (Array.isArray(o['data'])) return o['data'] as Project[];
    if (Array.isArray(o['content'])) return o['content'] as Project[];
  }
  return [];
}

Chart.register(...registerables);

@Component({
  selector: 'app-list-projects',
  standalone: true,
  imports: [RouterLink, CommonModule, FormsModule, BaseChartDirective],
  templateUrl: './list-projects.html',
  styleUrl: './list-projects.scss',
})
export class ListProjects implements OnInit, OnDestroy {

  // Only for admin statistics
  statusChartData: ChartData<'doughnut', number[], string> = {
    labels: ['OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'],
    datasets: [
      {
        data: [0, 0, 0, 0],
        backgroundColor: ['#36A2EB', '#FFCE56', '#4BC0C0', '#FF6384'],
        borderWidth: 1,
      },
    ],
  };

  statusChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    plugins: {
      legend: {
        position: 'bottom',
      },
      tooltip: {
        enabled: true,
      },
    },
  };

  canManageProjects = false;

  searchTerm: string = '';
  selectedStatus: string = 'ALL';
  filteredProjects: Project[] = [];

  projects: Project[] = [];
  isLoading = false;
  errorMessage: string | null = null;
  projectToDelete: Project | null = null;
  deleting = false;
  private autoRefreshSub: Subscription | null = null;
  private routerSub: Subscription | null = null;
  private previousUrl = '';

  constructor(
    private projectService: ProjectService,
    private authService: AuthService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  public userRole: string | null = null;

  ngOnInit(): void {
    this.previousUrl = this.router.url;

    this.userRole = this.authService.getUserRole();
    this.canManageProjects = this.userRole === 'CLIENT';

    this.loadProjects();
    this.autoRefreshSub = interval(AUTO_REFRESH_INTERVAL_MS).subscribe(() => {
      if (!this.isLoading) this.loadProjects();
    });
    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        if (this.previousUrl.includes('edit') && e.urlAfterRedirects.includes('my-projects')) {
          this.loadProjects();
        }
        this.previousUrl = e.urlAfterRedirects;
      });
  }

  ngOnDestroy(): void {
    this.autoRefreshSub?.unsubscribe();
    this.routerSub?.unsubscribe();
  }

  openDeleteModal(project: Project): void {
    this.projectToDelete = project;
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.projectToDelete = null;
  }

  doDelete(): void {
    const project = this.projectToDelete;
    if (!project?.id) return;
    this.deleting = true;
    this.projectService.deleteProject(project.id).subscribe({
      next: () => {
        this.projectToDelete = null;
        this.deleting = false;
        this.loadProjects();
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        console.error('Delete failed', err);
        this.deleting = false;
        this.errorMessage = 'Failed to delete project.';
        this.cdr.detectChanges();
      },
    });
  }

  private isTimeoutError(err: unknown): boolean {
    if (err && typeof err === 'object') {
      const o = err as { name?: string; message?: string };
      return o.name === 'TimeoutError' || (typeof o.message === 'string' && o.message.includes('Timeout'));
    }
    return false;
  }

  loadProjects(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.cdr.detectChanges();

    const email = this.authService.getPreferredUsername();
    if (!email) {
      this.errorMessage = 'You must be signed in to view projects.';
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    const user$ = this.userService.getByEmail(email).pipe(catchError(() => of(null)));
    const projects$ = this.projectService.getAllProjects().pipe(map((res) => toProjectList(res)));

    forkJoin({ user: user$, projects: projects$ }).subscribe({
      next: ({ user, projects }) => {
        const list = Array.isArray(projects) ? projects : toProjectList(projects);
        
        if (this.userRole === 'CLIENT') {
          const clientId =
            user?.id != null ? user.id : null;

          this.projects =
            clientId != null
              ? list.filter((p) => Number(p.clientId) === Number(clientId))
              : [];
          this.applyFilters();

        } else if (this.userRole === 'FREELANCER') {

          // Freelancer sees only OPEN projects
          this.projects = list.filter((p) => p.status === 'OPEN');
          this.applyFilters();

        } else {

          // ADMIN sees all projects
          this.projects = list;
          this.applyFilters();


          // ADMIN Stat
          const statusCounts = { OPEN: 0, IN_PROGRESS: 0, COMPLETED: 0, CANCELLED: 0 };
          this.projects.forEach(p => {
            if (statusCounts[p.status as keyof typeof statusCounts] !== undefined) {
              statusCounts[p.status as keyof typeof statusCounts]++;
            }
          });

          this.statusChartData.datasets[0].data = [
            statusCounts.OPEN,
            statusCounts.IN_PROGRESS,
            statusCounts.COMPLETED,
            statusCounts.CANCELLED,
          ];
        }

        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage = this.isTimeoutError(err)
          ? 'Request timed out. Use Refresh to try again.'
          : 'Failed to load projects.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  getSkills(project: Project): string[] {
    const s = project.skillsRequiered;
    if (s == null || s === '') return [];
    if (Array.isArray(s)) return s.map((x) => String(x).trim()).filter(Boolean);
    return String(s)
      .split(',')
      .map((x) => x.trim())
      .filter(Boolean);
  }

  applyFilters(): void {
    let temp = [...this.projects];

    // Search filter
    if (this.searchTerm.trim() !== '') {
      const term = this.searchTerm.toLowerCase();
      temp = temp.filter(p =>
        p.title?.toLowerCase().includes(term) ||
        p.category?.toLowerCase().includes(term) ||
        p.description?.toLowerCase().includes(term)
      );
    }

    // Status filter
    if (this.selectedStatus !== 'ALL') {
      temp = temp.filter(p => p.status === this.selectedStatus);
    }

    this.filteredProjects = temp;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onStatusChange(): void {
    this.applyFilters();
  }
}
