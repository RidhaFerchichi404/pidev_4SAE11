import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { ProjectService, Project, ProjectApplication } from '../../../core/services/project.service';
import {
  PlanningService,
  ProgressUpdate,
  ProgressComment,
  ProgressUpdateRequest,
  FreelancerProgressStatsDto,
  PageResponse,
  StalledProjectDto,
  CalendarEventDto,
} from '../../../core/services/planning.service';
import { Card } from '../../../shared/components/card/card';
import { forkJoin, Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

const TITLE_MAX = 200;
const SEARCH_DEBOUNCE_MS = 350;
const PAGE_SIZE = 10;
const DESCRIPTION_MAX = 2000;

export interface ProjectWithDetails {
  project: Project;
  application?: ProjectApplication;
}

@Component({
  selector: 'app-progress-updates',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './progress-updates.html',
  styleUrl: './progress-updates.scss',
})
export class ProgressUpdates implements OnInit, OnDestroy {
  private searchTrigger$ = new Subject<void>();
  private searchSub: Subscription | null = null;

  projects: ProjectWithDetails[] = [];
  selectedProject: Project | null = null;
  updates: ProgressUpdate[] = [];
  commentsByUpdateId: Record<number, ProgressComment[]> = {};
  loading = true;
  loadingUpdates = false;
  errorMessage = '';
  currentUser: User | null = null;
  form: FormGroup;
  filterForm: FormGroup;
  /** Search/filter for the project list (when no project selected). */
  projectFilterForm: FormGroup;
  modalOpen = false;
  editing: ProgressUpdate | null = null;
  saving = false;
  deleteTarget: ProgressUpdate | null = null;
  deleting = false;

  /** Freelancer stats (your progress across all projects) */
  stats: FreelancerProgressStatsDto | null = null;
  statsLoading = false;

  /** Projects that are due or overdue for an update (no update in N days). */
  dueOrOverdueProjects: StalledProjectDto[] = [];
  dueLoading = false;

  /** Upcoming calendar events (Google Calendar integration). */
  calendarEvents: CalendarEventDto[] = [];
  calendarEventsLoading = false;

  /** Pagination for updates list (when project selected) */
  page = 0;
  size = PAGE_SIZE;
  totalElements = 0;
  totalPages = 0;

  readonly titleMax = TITLE_MAX;
  readonly descriptionMax = DESCRIPTION_MAX;

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private projectService: ProjectService,
    private planning: PlanningService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(TITLE_MAX)]],
      description: ['', [Validators.maxLength(DESCRIPTION_MAX)]],
      progressPercentage: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
      nextUpdateDue: [null as string | null],
    });
    this.filterForm = this.fb.group({ search: [''] });
    this.projectFilterForm = this.fb.group({ search: [''] });
  }

  /** Projects filtered by project list search (title, description, id). */
  get filteredProjects(): ProjectWithDetails[] {
    const q = this.projectFilterForm?.get('search')?.value?.trim()?.toLowerCase() ?? '';
    if (!q) return this.projects;
    return this.projects.filter((item) => {
      const p = item.project;
      const title = (p.title ?? '').toLowerCase();
      const desc = (p.description ?? '').toLowerCase();
      const idStr = (p.id ?? '').toString();
      return title.includes(q) || desc.includes(q) || idStr.includes(q);
    });
  }

  ngOnInit(): void {
    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.loading = false;
      this.errorMessage = 'You must be logged in.';
      this.cdr.detectChanges();
      return;
    }
    this.userService.getByEmail(email).subscribe((user) => {
      this.currentUser = user ?? null;
      if (this.currentUser) {
        this.loadProjects();
        this.loadFreelancerStats();
        this.loadCalendarEvents();
        this.searchSub = this.searchTrigger$.pipe(debounceTime(SEARCH_DEBOUNCE_MS)).subscribe(() => {
          this.page = 0;
          this.loadUpdatesForProject();
        });
      } else {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  loadFreelancerStats(): void {
    if (!this.currentUser?.id) return;
    this.statsLoading = true;
    this.planning.getStatsByFreelancer(this.currentUser.id).subscribe({
      next: (s) => {
        this.stats = s ?? null;
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadCalendarEvents(): void {
    this.calendarEventsLoading = true;
    const now = new Date();
    const timeMin = now.toISOString();
    const timeMax = new Date(now.getTime() + 60 * 24 * 60 * 60 * 1000).toISOString();
    const userId = this.currentUser?.id ?? this.auth.getUserId();
    const role = this.auth.getUserRole();
    this.planning.getCalendarEvents({ timeMin, timeMax, userId: userId ?? undefined, role: role ?? undefined }).subscribe({
      next: (events) => {
        this.calendarEvents = events ?? [];
        this.calendarEventsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.calendarEvents = [];
        this.calendarEventsLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onSearchChange(): void {
    this.searchTrigger$.next();
  }

  clearSearch(): void {
    this.filterForm.patchValue({ search: '' });
    this.page = 0;
    this.loadUpdatesForProject();
  }

  clearProjectSearch(): void {
    this.projectFilterForm.patchValue({ search: '' });
    this.cdr.detectChanges();
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.loadUpdatesForProject();
  }

  loadProjects(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.errorMessage = '';
    this.projectService.getApplicationsByFreelancer(this.currentUser.id).subscribe({
      next: (applications: ProjectApplication[]) => {
        const appList = applications || [];
        const getProjectId = (a: ProjectApplication & { project?: { id?: number } }) => a.projectId ?? a.project?.id;
        const uniqueProjectIds = [...new Set(appList.map(getProjectId).filter((id): id is number => id != null && !Number.isNaN(Number(id))))];
        if (uniqueProjectIds.length === 0) {
          this.projects = [];
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }
        const projectRequests = uniqueProjectIds.map((id) => this.projectService.getById(id));
        forkJoin(projectRequests).subscribe({
          next: (projectResults) => {
            this.projects = projectResults
              .filter((p): p is Project => p != null && p.id != null)
              .map((project) => ({
                project,
                application: appList.find((a: ProjectApplication & { project?: { id?: number } }) => (a.projectId ?? a.project?.id) === project.id),
              }));
            this.loadDueOrOverdue();
            const projectIdParam = this.route.snapshot.queryParamMap.get('projectId');
            if (projectIdParam) {
              const id = Number(projectIdParam);
              if (!Number.isNaN(id)) {
                const item = this.projects.find((p) => p.project.id === id);
                if (item) this.selectProject(item.project);
              }
            }
            this.loading = false;
            this.cdr.detectChanges();
          },
          error: () => {
            this.loading = false;
            this.errorMessage = 'Failed to load project details.';
            this.cdr.detectChanges();
          },
        });
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Failed to load your projects.';
        this.cdr.detectChanges();
      },
    });
  }

  loadDueOrOverdue(): void {
    if (this.projects.length === 0) {
      this.dueOrOverdueProjects = [];
      return;
    }
    const ids = new Set(this.projects.map((p) => p.project.id).filter((id): id is number => id != null));
    this.dueLoading = true;
    this.planning.getDueOrOverdueProjects(7).subscribe({
      next: (list) => {
        this.dueOrOverdueProjects = (list ?? []).filter((s) => ids.has(s.projectId));
        this.dueLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.dueLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  selectProject(project: Project): void {
    this.selectedProject = project;
    this.errorMessage = '';
    this.filterForm.patchValue({ search: '' });
    this.page = 0;
    this.loadUpdatesForProject();
    // Sync project deadline to calendar so freelancer sees it; they get a notification when first synced
    if (this.currentUser?.id != null && project?.id != null) {
      this.planning.syncProjectDeadlineToCalendar(project.id, this.currentUser.id).subscribe();
    }
  }

  backToProjects(): void {
    this.selectedProject = null;
    this.updates = [];
    this.commentsByUpdateId = {};
    this.totalElements = 0;
    this.totalPages = 0;
  }

  loadUpdatesForProject(): void {
    const projectId = this.selectedProject?.id;
    const freelancerId = this.currentUser?.id;
    if (!this.selectedProject || projectId == null || freelancerId == null) return;
    this.loadingUpdates = true;
    const search = this.filterForm.get('search')?.value?.trim() || null;
    this.planning.getFilteredProgressUpdates({
      projectId,
      freelancerId,
      search: search || null,
      page: this.page,
      size: this.size,
      sort: 'createdAt,desc',
    }).subscribe({
      next: (res: PageResponse<ProgressUpdate>) => {
        this.updates = res.content ?? [];
        this.totalElements = res.totalElements ?? 0;
        this.totalPages = res.totalPages ?? 0;
        this.size = res.size ?? this.size;
        this.page = res.number ?? 0;
        this.commentsByUpdateId = {};
        this.updates.forEach((u) => this.loadComments(u.id));
        this.loadingUpdates = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingUpdates = false;
        this.errorMessage = 'Failed to load progress updates.';
        this.cdr.detectChanges();
      },
    });
  }

  loadComments(progressUpdateId: number): void {
    this.planning.getCommentsByProgressUpdateId(progressUpdateId).subscribe({
      next: (list) => {
        this.commentsByUpdateId[progressUpdateId] = list ?? [];
        this.cdr.detectChanges();
      },
      error: () => {
        this.commentsByUpdateId[progressUpdateId] = [];
        this.cdr.detectChanges();
      },
    });
  }

  /** Minimum progress % allowed for this project (from previous updates). */
  minProgressHint = 0;

  openCreate(): void {
    this.editing = null;
    this.errorMessage = '';
    this.minProgressHint = this.updates.length ? Math.max(...this.updates.map((x) => x.progressPercentage)) : 0;
    this.form.reset({ title: '', description: '', progressPercentage: this.minProgressHint, nextUpdateDue: null });
    this.form.get('progressPercentage')?.setValidators([
      Validators.required,
      Validators.min(this.minProgressHint),
      Validators.max(100),
    ]);
    this.form.get('progressPercentage')?.updateValueAndValidity();
    this.modalOpen = true;
  }

  openEdit(u: ProgressUpdate): void {
    this.editing = u;
    this.minProgressHint =
      this.updates.filter((x) => x.id !== u.id).length > 0
        ? Math.max(...this.updates.filter((x) => x.id !== u.id).map((x) => x.progressPercentage))
        : 0;
    const nextDue = u.nextUpdateDue ? this.toDatetimeLocal(u.nextUpdateDue) : null;
    this.form.patchValue({
      title: u.title,
      description: u.description ?? '',
      progressPercentage: u.progressPercentage,
      nextUpdateDue: nextDue,
    });
    this.form.get('progressPercentage')?.setValidators([
      Validators.required,
      Validators.min(this.minProgressHint),
      Validators.max(100),
    ]);
    this.form.get('progressPercentage')?.updateValueAndValidity();
    this.modalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
    this.editing = null;
  }

  save(): void {
    const projectId = this.selectedProject?.id;
    if (!this.currentUser || !this.selectedProject || projectId == null || this.form.invalid) return;
    const v = this.form.value;
    const nextDueRaw = v.nextUpdateDue as string | null | undefined;
    const nextUpdateDue = nextDueRaw && String(nextDueRaw).trim() ? this.toIsoDateTime(nextDueRaw) : null;
    const request: ProgressUpdateRequest = {
      projectId,
      contractId: null,
      freelancerId: this.currentUser.id,
      title: (v.title as string).trim(),
      description: (v.description as string)?.trim() || null,
      progressPercentage: Number(v.progressPercentage),
      nextUpdateDue,
    };
    this.saving = true;
    if (this.editing) {
      this.planning.updateProgressUpdate(this.editing.id, request).subscribe({
        next: (updated) => {
          this.saving = false;
          if (updated) {
            this.closeModal();
            this.loadUpdatesForProject();
          } else {
            this.errorMessage = 'Failed to update.';
          }
          this.cdr.detectChanges();
        },
        error: (err: { status?: number; error?: { message?: string; minAllowed?: number }; message?: string }) => {
          this.saving = false;
          const msg =
            err?.status === 400 && err?.error?.message
              ? String(err.error.message)
              : 'Failed to update.';
          this.errorMessage = msg;
          this.cdr.detectChanges();
        },
      });
    } else {
      this.planning.createProgressUpdate(request).subscribe({
        next: (created) => {
          this.saving = false;
          this.errorMessage = '';
          this.closeModal();
          this.loadUpdatesForProject();
          this.cdr.detectChanges();
        },
        error: (err: { status?: number; error?: { message?: string; error?: string; minAllowed?: number }; message?: string }) => {
          this.saving = false;
          const status = err?.status;
          let msg: string;
          if (status === 0) {
            msg = 'Cannot connect to the server. Start the API Gateway (port 8078) and the Planning service (port 8081), then try again.';
          } else if (status === 400 && err?.error?.message) {
            msg = String(err.error.message);
          } else {
            const body = err?.error?.message ?? err?.error?.error ?? err?.message;
            msg = body && typeof body === 'string' ? body : 'Failed to create progress update. Check that the Planning service is running.';
          }
          this.errorMessage = msg;
          this.cdr.detectChanges();
        },
      });
    }
  }

  confirmDelete(u: ProgressUpdate): void {
    this.deleteTarget = u;
  }

  cancelDelete(): void {
    this.deleteTarget = null;
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.deleting = true;
    this.planning.deleteProgressUpdate(this.deleteTarget.id).subscribe({
      next: (ok) => {
        this.deleting = false;
        this.deleteTarget = null;
        if (ok) this.loadUpdatesForProject();
        else this.errorMessage = 'Failed to delete.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.deleteTarget = null;
        this.errorMessage = 'Failed to delete.';
        this.cdr.detectChanges();
      },
    });
  }

  formatDate(s: string): string {
    if (!s) return '—';
    const d = new Date(s);
    return isNaN(d.getTime()) ? s : d.toLocaleString();
  }

  /** Convert ISO date-time to datetime-local value (YYYY-MM-DDTHH:mm). */
  toDatetimeLocal(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return '';
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const h = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${day}T${h}:${min}`;
  }

  /** Convert datetime-local or partial string to ISO for backend. */
  toIsoDateTime(local: string): string {
    if (!local || !String(local).trim()) return '';
    const s = String(local).trim();
    if (s.length <= 16) return s + (s.length === 16 ? ':00' : '');
    return s;
  }

  getTitleError(): string {
    const c = this.form.get('title');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'Title is required.';
    if (c.errors['maxlength']) return `Maximum ${TITLE_MAX} characters.`;
    return '';
  }

  getDescriptionError(): string {
    const c = this.form.get('description');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['maxlength']) return `Maximum ${DESCRIPTION_MAX} characters.`;
    return '';
  }

  getProgressError(): string {
    const c = this.form.get('progressPercentage');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'Progress is required.';
    if (c.errors['min'] != null) return `Minimum ${this.minProgressHint}% for this project (cannot be less than previous update).`;
    if (c.errors['max'] != null) return 'Maximum 100%.';
    return '';
  }
}
