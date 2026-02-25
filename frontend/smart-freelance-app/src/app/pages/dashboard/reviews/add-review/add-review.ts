import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService, User } from '../../../../core/services/user.service';
import { ProjectService, Project } from '../../../../core/services/project.service';
import { ReviewService, Review } from '../../../../core/services/review.service';
import { of } from 'rxjs';

@Component({
  selector: 'app-add-review',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-review.html',
  styleUrl: './add-review.scss',
})
export class AddReview implements OnInit {
  form!: FormGroup;
  isSubmitting = false;
  submitError: string | null = null;
  submitSuccess = false;
  currentUser: User | null = null;
  /** Projects the user can leave a review for (has other party and not yet reviewed) */
  projectOptions: { project: Project; revieweeId: number; label: string }[] = [];
  fromProject: { projectId: number; revieweeId: number } | null = null;
  revieweeLabel = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private auth: AuthService,
    private userService: UserService,
    private projectService: ProjectService,
    private reviewService: ReviewService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      projectId: [null as number | null, [Validators.required]],
      rating: [null as number | null, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: ['', [Validators.maxLength(1000)]],
    });

    const state = (typeof history !== 'undefined' && history.state) ? history.state : null;
    if (state?.projectId != null && state?.revieweeId != null) {
      this.fromProject = { projectId: Number(state.projectId), revieweeId: Number(state.revieweeId) };
      this.form.patchValue({ projectId: this.fromProject.projectId });
      this.form.get('projectId')?.clearValidators();
      this.form.get('projectId')?.updateValueAndValidity();
      if (state.revieweeLabel) this.revieweeLabel = state.revieweeLabel;
      else this.loadRevieweeLabel(this.fromProject.revieweeId);
    }

    const email = this.auth.getPreferredUsername();
    if (!email) {
      this.submitError = 'You must be logged in.';
      return;
    }
    this.userService.getByEmail(email).subscribe((user) => {
      this.currentUser = user ?? null;
      if (!this.currentUser) {
        this.submitError = 'Could not load your profile.';
        this.cdr.detectChanges();
        return;
      }
      if (!this.fromProject) this.loadProjectOptions();
      else this.cdr.detectChanges();
    });
  }

  private loadRevieweeLabel(revieweeId: number): void {
    this.userService.getById(revieweeId).subscribe((u) => {
      if (u) this.revieweeLabel = (u.firstName + ' ' + u.lastName).trim() || u.email;
      else this.revieweeLabel = 'User #' + revieweeId;
      this.cdr.detectChanges();
    });
  }

  private loadProjectOptions(): void {
    if (!this.currentUser?.id) return;
    const byClient = this.currentUser.role === 'CLIENT'
      ? this.projectService.getByClientId(this.currentUser.id)
      : of([] as Project[]);
    const byFreelancer = this.currentUser.role === 'FREELANCER'
      ? this.projectService.getByFreelancerId(this.currentUser.id)
      : of([] as Project[]);
    const projects$ = this.currentUser.role === 'CLIENT' ? byClient : byFreelancer;
    projects$.subscribe((projects: Project[] | undefined) => {
      this.reviewService.getByReviewerId(this.currentUser!.id).subscribe((myReviews) => {
        const reviewedProjectIds = new Set((myReviews ?? []).map((r) => r.projectId));
        const options: { project: Project; revieweeId: number; label: string }[] = [];
        (projects ?? []).forEach((p: Project) => {
          if (reviewedProjectIds.has(Number(p.id))) return;
          const revieweeId = this.currentUser!.role === 'CLIENT'
            ? p.freelancerId
            : p.clientId;
          if (revieweeId == null) return;
          const label = p.title + ' (Project #' + p.id + ')';
          options.push({ project: p, revieweeId: Number(revieweeId), label });
        });
        this.projectOptions = options;
        if (options.length === 1 && !this.fromProject) {
          this.form.patchValue({ projectId: options[0].project.id });
        }
        this.cdr.detectChanges();
      });
    });
  }

  submit(): void {
    if (this.form.invalid || !this.currentUser?.id) {
      this.form.markAllAsTouched();
      return;
    }
    const projectId = this.form.value.projectId ?? this.fromProject?.projectId;
    const revieweeId = this.fromProject?.revieweeId ?? this.projectOptions.find((o) => Number(o.project.id) === Number(projectId))?.revieweeId;
    if (projectId == null || revieweeId == null) {
      this.submitError = 'Please select a project.';
      return;
    }
    this.isSubmitting = true;
    this.submitError = null;
    const payload: Review = {
      reviewerId: this.currentUser.id,
      revieweeId,
      projectId: Number(projectId),
      rating: Number(this.form.value.rating),
      comment: (this.form.value.comment || '').trim() || undefined,
    };
    this.reviewService.create(payload).subscribe({
      next: () => {
        this.submitSuccess = true;
        this.isSubmitting = false;
        setTimeout(() => this.router.navigateByUrl('/dashboard/reviews'), 1500);
      },
      error: () => {
        this.submitError = 'Failed to submit review. Please try again.';
        this.isSubmitting = false;
      },
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/dashboard/reviews');
  }
}
