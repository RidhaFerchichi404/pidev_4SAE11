import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { ProjectService, Project, ProjectApplication } from '../../../core/services/project.service';
import {
  PlanningService,
  ProgressUpdate,
  ProgressComment,
  ProgressUpdateRequest,
} from '../../../core/services/planning.service';
import { Card } from '../../../shared/components/card/card';
import { forkJoin } from 'rxjs';

const TITLE_MAX = 200;
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
export class ProgressUpdates implements OnInit {
  projects: ProjectWithDetails[] = [];
  selectedProject: Project | null = null;
  updates: ProgressUpdate[] = [];
  commentsByUpdateId: Record<number, ProgressComment[]> = {};
  loading = true;
  loadingUpdates = false;
  errorMessage = '';
  currentUser: User | null = null;
  form: FormGroup;
  modalOpen = false;
  editing: ProgressUpdate | null = null;
  saving = false;
  deleteTarget: ProgressUpdate | null = null;
  deleting = false;

  readonly titleMax = TITLE_MAX;
  readonly descriptionMax = DESCRIPTION_MAX;

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private projectService: ProjectService,
    private planning: PlanningService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(TITLE_MAX)]],
      description: ['', [Validators.maxLength(DESCRIPTION_MAX)]],
      progressPercentage: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
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
      } else {
        this.loading = false;
        this.errorMessage = 'Could not load your profile.';
        this.cdr.detectChanges();
      }
    });
  }

  loadProjects(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.errorMessage = '';
    this.projectService.getApplicationsByFreelancer(this.currentUser.id).subscribe({
      next: (applications) => {
        const uniqueProjectIds = [...new Set((applications || []).map((a) => a.projectId))];
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
              .filter((p): p is Project => p != null)
              .map((project) => ({
                project,
                application: applications.find((a) => a.projectId === project.id),
              }));
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

  selectProject(project: Project): void {
    this.selectedProject = project;
    this.errorMessage = '';
    this.loadUpdatesForProject();
  }

  backToProjects(): void {
    this.selectedProject = null;
    this.updates = [];
    this.commentsByUpdateId = {};
  }

  loadUpdatesForProject(): void {
    if (!this.selectedProject) return;
    this.loadingUpdates = true;
    this.planning.getProgressUpdatesByProjectId(this.selectedProject.id).subscribe({
      next: (list) => {
        this.updates = (list || []).filter((u) => u.freelancerId === this.currentUser?.id);
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

  openCreate(): void {
    this.editing = null;
    this.form.reset({ title: '', description: '', progressPercentage: 0 });
    this.modalOpen = true;
  }

  openEdit(u: ProgressUpdate): void {
    this.editing = u;
    this.form.patchValue({
      title: u.title,
      description: u.description ?? '',
      progressPercentage: u.progressPercentage,
    });
    this.modalOpen = true;
  }

  closeModal(): void {
    this.modalOpen = false;
    this.editing = null;
  }

  save(): void {
    if (!this.currentUser || !this.selectedProject || this.form.invalid) return;
    const v = this.form.value;
    const request: ProgressUpdateRequest = {
      projectId: this.selectedProject.id,
      contractId: null,
      freelancerId: this.currentUser.id,
      title: (v.title as string).trim(),
      description: (v.description as string)?.trim() || null,
      progressPercentage: Number(v.progressPercentage),
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
        error: () => {
          this.saving = false;
          this.errorMessage = 'Failed to update.';
          this.cdr.detectChanges();
        },
      });
    } else {
      this.planning.createProgressUpdate(request).subscribe({
        next: (created) => {
          this.saving = false;
          if (created) {
            this.closeModal();
            this.loadUpdatesForProject();
          } else {
            this.errorMessage = 'Failed to create.';
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.saving = false;
          this.errorMessage = 'Failed to create.';
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
    if (c.errors['min'] != null) return 'Minimum 0%.';
    if (c.errors['max'] != null) return 'Maximum 100%.';
    return '';
  }
}
