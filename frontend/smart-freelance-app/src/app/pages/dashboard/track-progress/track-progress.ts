import { Component, ChangeDetectorRef, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { ProjectService, Project } from '../../../core/services/project.service';
import {
  PlanningService,
  ProgressUpdate,
  ProgressComment,
  ProgressCommentRequest,
  PageResponse,
} from '../../../core/services/planning.service';
import { Card } from '../../../shared/components/card/card';
import { forkJoin, Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

const MESSAGE_MAX = 2000;
const SEARCH_DEBOUNCE_MS = 350;
const PAGE_SIZE = 10;

/** Aggregated stats for client (across their projects). */
export interface ClientProgressStats {
  totalProjects: number;
  totalUpdates: number;
  totalComments: number;
  averageProgressPercentage: number | null;
}

@Component({
  selector: 'app-track-progress',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './track-progress.html',
  styleUrl: './track-progress.scss',
})
export class TrackProgress implements OnInit, OnDestroy {
  private searchTrigger$ = new Subject<void>();
  private searchSub: Subscription | null = null;

  currentUser: User | null = null;
  projects: Project[] = [];
  selectedProject: Project | null = null;
  updates: ProgressUpdate[] = [];
  commentsByUpdateId: Record<number, ProgressComment[]> = {};
  loading = true;
  loadingUpdates = false;
  loadingComments: Record<number, boolean> = {};
  errorMessage = '';
  addCommentForm: FormGroup;
  addCommentForUpdateId: number | null = null;
  editingComment: ProgressComment | null = null;
  editCommentForm: FormGroup;
  deleteCommentTarget: ProgressComment | null = null;
  saving = false;
  deleting = false;

  /** Search/filter for project list (when no project selected). */
  projectFilterForm: FormGroup;
  /** Search/filter for progress updates (when project selected). */
  filterForm: FormGroup;

  /** Client stats aggregated across their projects. */
  stats: ClientProgressStats | null = null;
  statsLoading = false;

  page = 0;
  size = PAGE_SIZE;
  totalElements = 0;
  totalPages = 0;

  readonly messageMax = MESSAGE_MAX;

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private projectService: ProjectService,
    private planning: PlanningService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.addCommentForm = this.fb.group({
      message: ['', [Validators.required, Validators.maxLength(MESSAGE_MAX)]],
    });
    this.editCommentForm = this.fb.group({
      message: ['', [Validators.required, Validators.maxLength(MESSAGE_MAX)]],
    });
    this.projectFilterForm = this.fb.group({ search: [''] });
    this.filterForm = this.fb.group({ search: [''] });
  }

  /** Projects filtered by project list search. */
  get filteredProjects(): Project[] {
    const q = this.projectFilterForm?.get('search')?.value?.trim()?.toLowerCase() ?? '';
    if (!q) return this.projects;
    return this.projects.filter((p) => {
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
      this.cdr.detectChanges();
      return;
    }
    this.userService.getByEmail(email).subscribe((user) => {
      this.currentUser = user ?? null;
      if (this.currentUser) {
        this.loadProjects();
        this.searchSub = this.searchTrigger$.pipe(debounceTime(SEARCH_DEBOUNCE_MS)).subscribe(() => {
          this.page = 0;
          this.loadUpdatesForProject();
        });
      } else {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  loadProjects(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.errorMessage = '';
    this.projectService.getByClientId(this.currentUser.id).subscribe({
      next: (list: Project[]) => {
        this.projects = list ?? [];
        this.loading = false;
        this.loadClientStats();
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Failed to load your projects.';
        this.cdr.detectChanges();
      },
    });
  }

  loadClientStats(): void {
    if (this.projects.length === 0) {
      this.stats = { totalProjects: 0, totalUpdates: 0, totalComments: 0, averageProgressPercentage: null };
      this.statsLoading = false;
      return;
    }
    this.statsLoading = true;
    const requests = this.projects.map((p) => this.planning.getStatsByProject(p.id!));
    forkJoin(requests).subscribe({
      next: (results) => {
        let totalUpdates = 0;
        let totalComments = 0;
        let sumProgress = 0;
        let countProgress = 0;
        results.forEach((r) => {
          if (r) {
            totalUpdates += r.updateCount ?? 0;
            totalComments += r.commentCount ?? 0;
            if (r.currentProgressPercentage != null) {
              sumProgress += r.currentProgressPercentage;
              countProgress += 1;
            }
          }
        });
        this.stats = {
          totalProjects: this.projects.length,
          totalUpdates,
          totalComments,
          averageProgressPercentage: countProgress > 0 ? sumProgress / countProgress : null,
        };
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.statsLoading = false;
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

  selectProject(project: Project): void {
    this.selectedProject = project;
    this.errorMessage = '';
    this.filterForm.patchValue({ search: '' });
    this.page = 0;
    this.loadUpdatesForProject();
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
    if (!this.selectedProject || projectId == null) return;
    this.loadingUpdates = true;
    const search = this.filterForm.get('search')?.value?.trim() || null;
    this.planning.getFilteredProgressUpdates({
      projectId,
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
    this.loadingComments[progressUpdateId] = true;
    this.planning.getCommentsByProgressUpdateId(progressUpdateId).subscribe({
      next: (list) => {
        const comments = (list ?? []).map((c) => ({ ...c, progressUpdateId }));
        this.commentsByUpdateId[progressUpdateId] = comments;
        this.loadingComments[progressUpdateId] = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.commentsByUpdateId[progressUpdateId] = [];
        this.loadingComments[progressUpdateId] = false;
        this.cdr.detectChanges();
      },
    });
  }

  openAddComment(updateId: number): void {
    this.addCommentForUpdateId = updateId;
    this.addCommentForm.reset({ message: '' });
  }

  cancelAddComment(): void {
    this.addCommentForUpdateId = null;
  }

  submitComment(): void {
    if (!this.currentUser || this.addCommentForm.invalid || this.addCommentForUpdateId == null) return;
    const progressUpdateId = this.addCommentForUpdateId;
    const request: ProgressCommentRequest = {
      progressUpdateId,
      userId: this.currentUser.id,
      message: (this.addCommentForm.value.message as string).trim(),
    };
    this.saving = true;
    this.planning.createComment(request).subscribe({
      next: () => {
        this.saving = false;
        this.addCommentForUpdateId = null;
        this.loadComments(progressUpdateId);
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Failed to add comment.';
        this.cdr.detectChanges();
      },
    });
  }

  openEditComment(comment: ProgressComment): void {
    this.editingComment = comment;
    this.editCommentForm.patchValue({ message: comment.message });
  }

  cancelEditComment(): void {
    this.editingComment = null;
  }

  saveEditComment(): void {
    if (!this.editingComment || this.editCommentForm.invalid) return;
    const updateId = this.editingComment.progressUpdateId;
    this.saving = true;
    this.planning.updateComment(this.editingComment.id, { message: (this.editCommentForm.value.message as string).trim() }).subscribe({
      next: () => {
        this.saving = false;
        this.editingComment = null;
        if (updateId != null) this.loadComments(updateId);
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Failed to update comment.';
        this.cdr.detectChanges();
      },
    });
  }

  confirmDeleteComment(comment: ProgressComment): void {
    this.deleteCommentTarget = comment;
  }

  cancelDeleteComment(): void {
    this.deleteCommentTarget = null;
  }

  doDeleteComment(): void {
    if (!this.deleteCommentTarget) return;
    const updateId = this.deleteCommentTarget.progressUpdateId;
    this.deleting = true;
    this.planning.deleteComment(this.deleteCommentTarget.id).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteCommentTarget = null;
        if (updateId != null) this.loadComments(updateId);
        this.cdr.detectChanges();
      },
      error: () => {
        this.deleting = false;
        this.deleteCommentTarget = null;
        this.errorMessage = 'Failed to delete comment.';
        this.cdr.detectChanges();
      },
    });
  }

  formatDate(s: string): string {
    if (!s) return '—';
    const d = new Date(s);
    return isNaN(d.getTime()) ? s : d.toLocaleString();
  }

  isMyComment(comment: ProgressComment): boolean {
    return this.currentUser != null && comment.userId === this.currentUser.id;
  }

  isEditingCommentInUpdate(updateId: number): boolean {
    return this.editingComment != null && this.editingComment.progressUpdateId === updateId;
  }

  getAddMessageError(): string {
    const c = this.addCommentForm.get('message');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'Comment is required.';
    if (c.errors['maxlength']) return `Maximum ${MESSAGE_MAX} characters.`;
    return '';
  }

  getEditMessageError(): string {
    const c = this.editCommentForm.get('message');
    if (!c?.touched || !c?.errors) return '';
    if (c.errors['required']) return 'Comment is required.';
    if (c.errors['maxlength']) return `Maximum ${MESSAGE_MAX} characters.`;
    return '';
  }
}
