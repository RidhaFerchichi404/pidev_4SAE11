import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, Subscription, forkJoin, of } from 'rxjs';
import { debounceTime, catchError, switchMap } from 'rxjs/operators';
import {
  PlanningService,
  ProgressUpdate,
  ProgressUpdateRequest,
  ProgressUpdateFilterParams,
  PageResponse,
  DashboardStatsDto,
  StalledProjectDto,
  FreelancerActivityDto,
  ProjectActivityDto,
} from '../../../core/services/planning.service';
import { UserService, User } from '../../../core/services/user.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import { ProjectApplicationService } from '../../../core/services/project-application.service';
import { Card } from '../../../shared/components/card/card';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

Chart.register(...registerables);

const SEARCH_DEBOUNCE_MS = 350;

@Component({
  selector: 'app-planning-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card, BaseChartDirective],
  templateUrl: './planning-management.html',
  styleUrl: './planning-management.scss',
})
export class PlanningManagement implements OnInit, OnDestroy {
  private searchTrigger$ = new Subject<void>();
  private searchSub: Subscription | null = null;
  updates: ProgressUpdate[] = [];
  loading = true;
  errorMessage = '';
  updateToDelete: ProgressUpdate | null = null;
  deleting = false;
  showUpdate: ProgressUpdate | null = null;
  addForm: FormGroup;
  editForm: FormGroup;
  filterForm: FormGroup;
  addModalOpen = false;
  editingUpdate: ProgressUpdate | null = null;
  saving = false;
  adding = false;

  /** Pagination (planning microservice) */
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;

  /** Statistics from planning microservice only */
  dashboardStats: DashboardStatsDto | null = null;
  statsLoading = false;
  stalledProjects: StalledProjectDto[] = [];
  topFreelancers: FreelancerActivityDto[] = [];
  mostActiveProjects: ProjectActivityDto[] = [];

  /** Resolved names for display (from User and Project services) */
  freelancerIdToName: Record<number, string> = {};
  projectIdToTitle: Record<number, string> = {};

  /** Lists for Add/Edit form dropdowns (name + ID) */
  projectsForAdd: Project[] = [];
  freelancersForAdd: User[] = [];

  /** Min progress % for Add/Edit (cannot be less than previous update for the project) */
  addMinProgress = 0;
  editMinProgress = 0;
  /** API error message for progress (shown on form when 400 with message) */
  addProgressError = '';
  editProgressError = '';

  /** Chart data for overview (bar) */
  overviewChartData: ChartData<'bar'> = {
    labels: ['Total updates', 'Total comments', 'Projects', 'Freelancers'],
    datasets: [{ data: [0, 0, 0, 0], label: 'Count', backgroundColor: ['#36A2EB', '#FFCE56', '#4BC0C0', '#FF6384'] }],
  };
  overviewChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true } },
  };

  /** Chart data for top freelancers (horizontal bar) */
  topFreelancersChartData: ChartData<'bar'> = { labels: [], datasets: [{ data: [], label: 'Updates', backgroundColor: '#36A2EB' }] };
  topFreelancersChartOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { x: { beginAtZero: true } },
  };

  /** Chart data for most active projects (horizontal bar) */
  mostActiveProjectsChartData: ChartData<'bar'> = { labels: [], datasets: [{ data: [], label: 'Updates', backgroundColor: '#4BC0C0' }] };
  mostActiveProjectsChartOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { x: { beginAtZero: true } },
  };

  constructor(
    private planningService: PlanningService,
    private userService: UserService,
    private projectService: ProjectService,
    private projectApplicationService: ProjectApplicationService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.addForm = this.fb.group({
      projectId: [null as number | null, [Validators.required, Validators.min(1)]],
      freelancerId: [null as number | null, [Validators.required, Validators.min(1)]],
      title: ['', Validators.required],
      description: [''],
      progressPercentage: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    });
    this.editForm = this.fb.group({
      projectId: [null as number | null, [Validators.required, Validators.min(1)]],
      freelancerId: [null as number | null, [Validators.required, Validators.min(1)]],
      title: ['', Validators.required],
      description: [''],
      progressPercentage: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    });
    this.filterForm = this.fb.group({
      search: [''],
    });
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadUpdates();
    this.searchSub = this.searchTrigger$.pipe(
      debounceTime(SEARCH_DEBOUNCE_MS)
    ).subscribe(() => {
      this.page = 0;
      this.loadUpdates();
    });
    // When project is selected in Add form, auto-fill freelancer from progress updates or project applications
    this.addForm.get('projectId')?.valueChanges.pipe(
      switchMap((projectId: number | null) => {
        if (projectId == null) return of(null);
        return this.planningService.getProgressUpdatesByProjectId(projectId).pipe(
          catchError(() => of([])),
          switchMap((updates) => {
            if (updates.length > 0) {
              const sorted = [...updates].sort((a, b) =>
                new Date(b.updatedAt ?? b.createdAt).getTime() - new Date(a.updatedAt ?? a.createdAt).getTime()
              );
              return of(sorted[0].freelancerId);
            }
            return this.projectApplicationService.getApplicationsByProject(projectId).pipe(
              catchError(() => of([])),
              switchMap((apps) => {
                if (apps.length === 0) return of(null);
                const accepted = apps.find((a) => a.status === 'ACCEPTED');
                const app = accepted ?? apps[0];
                return of(app.freelanceId);
              })
            );
          })
        );
      })
    ).subscribe((freelancerId: number | null) => {
      if (freelancerId != null) {
        this.addForm.patchValue({ freelancerId }, { emitEvent: false });
        this.cdr.detectChanges();
      }
    });
    // When freelancer is selected in Add form, auto-fill project from progress updates or project applications
    this.addForm.get('freelancerId')?.valueChanges.pipe(
      switchMap((freelancerId: number | null) => {
        if (freelancerId == null) return of(null);
        return this.planningService.getProgressUpdatesByFreelancerId(freelancerId).pipe(
          catchError(() => of([])),
          switchMap((updates) => {
            if (updates.length > 0) {
              const sorted = [...updates].sort((a, b) =>
                new Date(b.updatedAt ?? b.createdAt).getTime() - new Date(a.updatedAt ?? a.createdAt).getTime()
              );
              return of(sorted[0].projectId);
            }
            return this.projectApplicationService.getApplicationsByFreelance(freelancerId).pipe(
              catchError(() => of([])),
              switchMap((apps) => {
                if (apps.length === 0) return of(null);
                const accepted = apps.find((a) => a.status === 'ACCEPTED');
                const app = accepted ?? apps[0];
                return of(app.projectId);
              })
            );
          })
        );
      })
    ).subscribe((projectId: number | null) => {
      if (projectId != null) {
        this.addForm.patchValue({ projectId }, { emitEvent: false });
        this.cdr.detectChanges();
      }
    });
    // When Add form projectId is set, fetch max progress for that project and enforce min
    this.addForm.get('projectId')?.valueChanges.subscribe((projectId: number | null) => {
      this.addProgressError = '';
      if (projectId == null) {
        this.addMinProgress = 0;
        this.setAddProgressValidators(0);
        return;
      }
      this.planningService.getProgressUpdatesByProjectId(projectId).pipe(catchError(() => of([]))).subscribe((updates) => {
        const max = updates.length > 0 ? Math.max(...updates.map((u) => u.progressPercentage)) : 0;
        this.addMinProgress = max;
        this.setAddProgressValidators(max);
        const current = this.addForm.get('progressPercentage')?.value;
        if (typeof current === 'number' && current < max) {
          this.addForm.patchValue({ progressPercentage: max }, { emitEvent: false });
        }
        this.cdr.detectChanges();
      });
    });
  }

  private setAddProgressValidators(minVal: number): void {
    this.addForm.get('progressPercentage')?.setValidators([
      Validators.required,
      Validators.min(minVal),
      Validators.max(100),
    ]);
    this.addForm.get('progressPercentage')?.updateValueAndValidity();
  }

  private setEditProgressValidators(minVal: number): void {
    this.editForm.get('progressPercentage')?.setValidators([
      Validators.required,
      Validators.min(minVal),
      Validators.max(100),
    ]);
    this.editForm.get('progressPercentage')?.updateValueAndValidity();
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  /** Called on search input (same pattern as project list) – triggers debounced AJAX. */
  onSearchChange(): void {
    this.searchTrigger$.next();
  }

  private buildFilterParams(): ProgressUpdateFilterParams {
    const v = this.filterForm.getRawValue();
    return {
      page: this.page,
      size: this.size,
      sort: 'createdAt,desc',
      search: v.search?.trim() || null,
    };
  }

  loadStats(): void {
    this.statsLoading = true;
    const users$ = this.userService.getAll().pipe(catchError(() => of([])));
    const projects$ = this.projectService.getAllProjects().pipe(catchError(() => of([])));
    forkJoin({
      stats: this.planningService.getDashboardStats().pipe(catchError(() => of(null))),
      stalled: this.planningService.getStalledProjects(7).pipe(catchError(() => of([]))),
      freelancers: this.planningService.getFreelancersByActivity(5).pipe(catchError(() => of([]))),
      projects: this.planningService.getMostActiveProjects(5).pipe(catchError(() => of([]))),
      users: users$,
      projectsList: projects$,
    }).subscribe({
      next: ({ stats, stalled, freelancers, projects, users, projectsList }) => {
        this.dashboardStats = stats ?? null;
        this.stalledProjects = stalled ?? [];
        this.topFreelancers = freelancers ?? [];
        this.mostActiveProjects = projects ?? [];
        this.freelancerIdToName = {};
        (users ?? []).forEach((u) => {
          this.freelancerIdToName[u.id] = `${u.firstName} ${u.lastName}`.trim() || `User ${u.id}`;
        });
        this.projectIdToTitle = {};
        (projectsList ?? []).forEach((p) => {
          if (p.id != null) this.projectIdToTitle[p.id] = p.title?.trim() || `Project ${p.id}`;
        });
        this.projectsForAdd = (projectsList ?? []).filter((p) => p.id != null);
        this.freelancersForAdd = (users ?? []).filter((u) => u.role === 'FREELANCER');
        this.updateOverviewChart();
        this.updateTopFreelancersChart();
        this.updateMostActiveProjectsChart();
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private updateOverviewChart(): void {
    if (!this.dashboardStats) return;
    const s = this.dashboardStats;
    this.overviewChartData = {
      labels: ['Total updates', 'Total comments', 'Projects', 'Freelancers'],
      datasets: [{
        data: [s.totalUpdates, s.totalComments, s.distinctProjectCount, s.distinctFreelancerCount],
        label: 'Count',
        backgroundColor: ['#36A2EB', '#FFCE56', '#4BC0C0', '#FF6384'],
      }],
    };
  }

  private updateTopFreelancersChart(): void {
    const labels = this.topFreelancers.map((f) => this.getFreelancerName(f.freelancerId));
    const data = this.topFreelancers.map((f) => f.updateCount);
    this.topFreelancersChartData = {
      labels,
      datasets: [{ data, label: 'Updates', backgroundColor: '#36A2EB' }],
    };
  }

  private updateMostActiveProjectsChart(): void {
    const labels = this.mostActiveProjects.map((p) => this.getProjectTitle(p.projectId));
    const data = this.mostActiveProjects.map((p) => p.updateCount);
    this.mostActiveProjectsChartData = {
      labels,
      datasets: [{ data, label: 'Updates', backgroundColor: '#4BC0C0' }],
    };
  }

  getFreelancerName(id: number): string {
    return this.freelancerIdToName[id] ?? `Freelancer ${id}`;
  }

  getProjectTitle(id: number): string {
    return this.projectIdToTitle[id] ?? `Project ${id}`;
  }

  /** Display label for user in dropdown: "FirstName LastName (#id)" */
  getFreelancerLabel(user: User): string {
    const name = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() || `User ${user.id}`;
    return `${name} (#${user.id})`;
  }

  /** Display label for project in dropdown: "Title (#id)" */
  getProjectLabel(project: Project): string {
    const title = project.title?.trim() || `Project ${project.id}`;
    return `${title} (#${project.id})`;
  }

  loadUpdates(): void {
    this.loading = true;
    this.errorMessage = '';
    const params = this.buildFilterParams();
    this.planningService.getFilteredProgressUpdates(params).subscribe({
      next: (page: PageResponse<ProgressUpdate>) => {
        this.updates = page.content ?? [];
        this.totalElements = page.totalElements ?? 0;
        this.totalPages = page.totalPages ?? 0;
        this.size = page.size ?? this.size;
        this.page = page.number ?? 0;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load progress updates.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  clearSearch(): void {
    this.filterForm.patchValue({ search: '' });
    this.page = 0;
    this.loadUpdates();
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.loadUpdates();
  }

  refreshAll(): void {
    this.loadStats();
    this.loadUpdates();
  }

  openAdd(): void {
    this.errorMessage = '';
    this.addProgressError = '';
    this.addMinProgress = 0;
    this.setAddProgressValidators(0);
    this.addForm.reset({
      projectId: null,
      freelancerId: null,
      title: '',
      description: '',
      progressPercentage: 0,
    });
    this.addModalOpen = true;
    // Ensure dropdowns have data if stats haven't loaded yet
    if (this.projectsForAdd.length === 0 || this.freelancersForAdd.length === 0) {
      forkJoin({
        projects: this.projectService.getAllProjects().pipe(catchError(() => of([]))),
        users: this.userService.getAll().pipe(catchError(() => of([]))),
      }).subscribe(({ projects, users }) => {
        this.projectsForAdd = (projects ?? []).filter((p) => p.id != null);
        this.freelancersForAdd = (users ?? []).filter((u) => u.role === 'FREELANCER');
        this.cdr.detectChanges();
      });
    }
  }

  closeAdd(): void {
    if (!this.adding) this.addModalOpen = false;
  }

  buildRequest(v: { projectId: number | null; freelancerId: number | null; title: string; description: string; progressPercentage: number }): ProgressUpdateRequest {
    return {
      projectId: Number(v.projectId),
      contractId: null,
      freelancerId: Number(v.freelancerId),
      title: v.title.trim(),
      description: v.description?.trim() || null,
      progressPercentage: Number(v.progressPercentage),
    };
  }

  saveAdd(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }
    const v = this.addForm.getRawValue();
    const request = this.buildRequest(v);
    this.adding = true;
    this.planningService.createProgressUpdate(request).subscribe({
      next: () => {
        this.adding = false;
        this.addModalOpen = false;
        this.refreshAll();
        this.cdr.detectChanges();
      },
      error: (err: { status?: number; error?: { message?: string; minAllowed?: number }; message?: string }) => {
        this.adding = false;
        const msg = err?.status === 400 && err?.error?.message ? String(err.error.message) : 'Failed to create progress update.';
        this.errorMessage = msg;
        if (err?.status === 400 && err?.error?.message && typeof err?.error?.minAllowed === 'number') {
          this.addProgressError = err.error.message;
          this.addMinProgress = err.error.minAllowed;
          this.setAddProgressValidators(this.addMinProgress);
          const current = this.addForm.get('progressPercentage')?.value;
          if (typeof current === 'number' && current < this.addMinProgress) {
            this.addForm.patchValue({ progressPercentage: this.addMinProgress }, { emitEvent: false });
          }
        }
        this.cdr.detectChanges();
      },
    });
  }

  openEdit(update: ProgressUpdate): void {
    this.editingUpdate = update;
    this.updateToDelete = null;
    this.editProgressError = '';
    this.editForm.patchValue({
      projectId: update.projectId,
      freelancerId: update.freelancerId,
      title: update.title,
      description: update.description ?? '',
      progressPercentage: update.progressPercentage,
    });
    this.planningService.getProgressUpdatesByProjectId(update.projectId).pipe(catchError(() => of([]))).subscribe((updates) => {
      const others = updates.filter((u) => u.id !== update.id);
      const max = others.length > 0 ? Math.max(...others.map((u) => u.progressPercentage)) : 0;
      this.editMinProgress = max;
      this.setEditProgressValidators(max);
      const current = this.editForm.get('progressPercentage')?.value;
      if (typeof current === 'number' && current < max) {
        this.editForm.patchValue({ progressPercentage: max }, { emitEvent: false });
      }
      this.cdr.detectChanges();
    });
  }

  closeEdit(): void {
    if (!this.saving) this.editingUpdate = null;
  }

  saveEdit(): void {
    if (!this.editingUpdate?.id || this.editForm.invalid) return;
    const v = this.editForm.getRawValue();
    const request = this.buildRequest(v);
    this.saving = true;
    this.planningService.updateProgressUpdate(this.editingUpdate.id, request).subscribe({
      next: (updated) => {
        this.saving = false;
        if (updated) {
          const idx = this.updates.findIndex((u) => u.id === this.editingUpdate!.id);
          if (idx !== -1) this.updates[idx] = { ...this.updates[idx], ...updated };
          this.editingUpdate = null;
        } else this.errorMessage = 'Failed to update progress update.';
        this.cdr.detectChanges();
      },
      error: (err: { status?: number; error?: { message?: string; minAllowed?: number }; message?: string }) => {
        this.saving = false;
        const msg = err?.status === 400 && err?.error?.message ? String(err.error.message) : 'Failed to update progress update.';
        this.errorMessage = msg;
        if (err?.status === 400 && err?.error?.message && typeof err?.error?.minAllowed === 'number') {
          this.editProgressError = err.error.message;
          this.editMinProgress = err.error.minAllowed;
          this.setEditProgressValidators(this.editMinProgress);
          const current = this.editForm.get('progressPercentage')?.value;
          if (typeof current === 'number' && current < this.editMinProgress) {
            this.editForm.patchValue({ progressPercentage: this.editMinProgress }, { emitEvent: false });
          }
        }
        this.cdr.detectChanges();
      },
    });
  }

  openShow(update: ProgressUpdate): void {
    this.showUpdate = update;
  }

  closeShow(): void {
    this.showUpdate = null;
  }

  /** Close show modal and open edit for the same update. */
  editShownUpdate(): void {
    const u = this.showUpdate;
    this.closeShow();
    if (u) this.openEdit(u);
  }

  openDeleteModal(update: ProgressUpdate): void {
    this.updateToDelete = update;
    this.closeEdit();
  }

  closeDeleteModal(): void {
    if (!this.deleting) this.updateToDelete = null;
  }

  doDelete(): void {
    if (!this.updateToDelete?.id) return;
    this.deleting = true;
    this.planningService.deleteProgressUpdate(this.updateToDelete.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.updateToDelete = null;
        if (ok) this.refreshAll();
        else this.errorMessage = 'Failed to delete progress update.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.errorMessage = 'Failed to delete progress update.';
        this.cdr.detectChanges();
      },
    });
  }
}
