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
import { ProjectApplicationService, ProjectApplicationStats } from '../../../core/services/project-application.service';


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
  statusChartData = {
    labels: ['Open', 'In Progress', 'Completed', 'Cancelled'],
    datasets: [{
      data: [0, 0, 0, 0],
      backgroundColor: [
        '#2563EB',
        '#F59E0B',
        '#10B981',
        '#6B7280'
      ],
      borderWidth: 0
    }]
  };

  statusChartOptions = {
    responsive: true,
    cutout: '75%',
    plugins: {
      legend: {
        display: false
      }
    }
  };

  // 🔥 Applications per Project (HORIZONTAL BAR CHART)
  applicationsChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [{
      label: 'Applications',
      data: [],
      backgroundColor: [],
      borderRadius: 6,
      barThickness: 20,
      maxBarThickness: 28
    }]
  };

  applicationsChartOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          title: (items) => {
            const idx = items[0]?.dataIndex ?? 0;
            const limit = this.applicationsChartLimit === 9999 ? this.applicationStats.length : this.applicationsChartLimit;
            const stat = this.applicationStats.slice(0, limit)[idx];
            return stat?.projectTitle ?? this.applicationsChartData.labels?.[idx] ?? '';
          },
          label: (ctx) => `${ctx.raw} application${Number(ctx.raw) !== 1 ? 's' : ''}`
        }
      }
    },
    scales: {
      x: {
        beginAtZero: true,
        ticks: { precision: 0, stepSize: 1 },
        grid: { color: 'rgba(0,0,0,0.06)' }
      },
      y: {
        grid: { display: false },
        ticks: { font: { size: 11 }, maxRotation: 0, autoSkip: false }
      }
    }
  };

  applicationStats: ProjectApplicationStats[] = [];
  applicationsChartLimit = 15;
  applicationsChartLimitOptions = [10, 15, 20, 30, 50, 9999];

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
    private applicationService: ProjectApplicationService,
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
          // 🔥 Get statistics from backend
          this.projectService.getProjectStatistics().subscribe(stats => {

            if (!stats) return;

            this.statusChartData = {
              ...this.statusChartData,
              datasets: [{
                ...this.statusChartData.datasets[0],
                data: [
                  stats.openProjects ?? 0,
                  stats.inProgressProjects ?? 0,
                  stats.completedProjects ?? 0,
                  stats.cancelledProjects ?? 0,
                ]
              }]
            };

            this.cdr.detectChanges();
          });

          // 🔥 Applications per Project (HORIZONTAL BAR)
          this.applicationService.getProjectApplicationStatistics()
            .subscribe(stats => {
              if (!stats || stats.length === 0) {
                this.applicationStats = [];
                this.applicationsChartData = {
                  labels: [],
                  datasets: [{ label: 'Applications', data: [], backgroundColor: [], borderRadius: 6 }]
                };
                this.cdr.detectChanges();
                return;
              }
              // Sort by count descending, most applied first
              const sorted = [...stats].sort((a, b) => b.applicationsCount - a.applicationsCount);
              this.applicationStats = sorted;
              this.updateApplicationsChart();
              this.cdr.detectChanges();
            });
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
    return project.skills?.map(s => s.name) ?? [];
  }

  exportPdf(): void {
    this.projectService.exportProjectsPdf().subscribe(blob => {

      const url = window.URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = url;
      a.download = 'projects.pdf';
      a.click();

      window.URL.revokeObjectURL(url);
    });
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

  onApplicationsChartLimitChange(): void {
    this.updateApplicationsChart();
  }

  private updateApplicationsChart(): void {
    const limit = this.applicationsChartLimit === 9999 ? this.applicationStats.length : this.applicationsChartLimit;
    const sliced = this.applicationStats.slice(0, limit);
    const maxCount = Math.max(...this.applicationStats.map(s => s.applicationsCount), 1);
    this.applicationsChartData = {
      labels: sliced.map(s => s.projectTitle.length > 50 ? s.projectTitle.slice(0, 47) + '…' : s.projectTitle),
      datasets: [{
        label: 'Applications',
        data: sliced.map(s => s.applicationsCount),
        backgroundColor: sliced.map(s => {
          const t = s.applicationsCount / maxCount;
          const r = Math.round(37 + (99 - 37) * (1 - t));
          const g = Math.round(99 + (179 - 99) * (1 - t));
          const b = Math.round(235);
          return `rgb(${r}, ${g}, ${b})`;
        }),
        borderRadius: 6,
        barThickness: 20,
        maxBarThickness: 28
      }]
    };
  }

  get totalApplicationsCount(): number {
    return this.applicationStats.reduce((acc, s) => acc + s.applicationsCount, 0);
  }

  get applicationsChartLimitLabel(): string {
    return this.applicationsChartLimit === 9999 ? 'All' : `Top ${this.applicationsChartLimit}`;
  }

  /** Dynamic height so each project bar has ~32px; enables scrolling when many projects. */
  get applicationsChartHeightPx(): number {
    const n = this.applicationsChartData?.labels?.length ?? 0;
    const minHeight = 320;
    const pxPerBar = 32;
    return Math.max(minHeight, n * pxPerBar);
  }

  get isBrowseJobs(): boolean {
    return this.router.url.includes('browse-jobs');
  }
}
