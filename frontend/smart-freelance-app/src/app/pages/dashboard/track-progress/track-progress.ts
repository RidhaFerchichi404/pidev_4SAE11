import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
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
} from '../../../core/services/planning.service';
import { Card } from '../../../shared/components/card/card';

const MESSAGE_MAX = 2000;

@Component({
  selector: 'app-track-progress',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card],
  templateUrl: './track-progress.html',
  styleUrl: './track-progress.scss',
})
export class TrackProgress implements OnInit {
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
      } else {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadProjects(): void {
    if (!this.currentUser?.id) return;
    this.loading = true;
    this.errorMessage = '';
    this.projectService.getByClientId(this.currentUser.id).subscribe({
      next: (list) => {
        this.projects = list ?? [];
        this.loading = false;
        this.cdr.detectChanges();
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
    this.updates = [];
    this.commentsByUpdateId = {};
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
        this.updates = list ?? [];
        this.loadingUpdates = false;
        this.commentsByUpdateId = {};
        this.updates.forEach((u) => this.loadComments(u.id));
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
