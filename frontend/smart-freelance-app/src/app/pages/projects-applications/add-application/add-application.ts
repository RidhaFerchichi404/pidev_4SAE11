import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectApplicationService } from '../../../core/services/project-application.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-add-application',
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './add-application.html',
  styleUrl: './add-application.scss',
})
export class AddApplication implements OnInit{
  applicationForm!: FormGroup;
  isSubmitting = false;
  submitSuccess = false;
  submitError: string | null = null;

  projectId!: number;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private applicationService: ProjectApplicationService,
    private authService: AuthService,
    private userService: UserService,
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));

    if (!this.projectId) {
      this.submitError = 'Invalid project.';
      return;
    }

    this.applicationForm = this.fb.group({
      proposedPrice: [null, [Validators.required, Validators.min(1)]],
      proposedDuration: [null, [Validators.required, Validators.min(1)]],
      coverLetter: ['', [Validators.required, Validators.minLength(20)]],
    });
  }

  submitApplication(): void {
    if (this.applicationForm.invalid) {
      this.applicationForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.submitError = null;

    const email = this.authService.getPreferredUsername();
    if (!email) {
      this.submitError = 'You must be signed in.';
      this.isSubmitting = false;
      return;
    }

    this.userService.getByEmail(email).subscribe({
      next: (user) => {
        if (!user?.id) {
          this.submitError = 'Could not identify your account.';
          this.isSubmitting = false;
          return;
        }

        const payload = {
          ...this.applicationForm.value,
          project: { id: this.projectId },
          freelanceId: user.id,
          status: 'PENDING',
        };

        this.applicationService.addApplication(payload).subscribe({
          next: () => {
            this.submitSuccess = true;
            this.isSubmitting = false;

            setTimeout(() => {
              this.router.navigateByUrl('/dashboard/my-applications');
            }, 1500);
          },
          error: (err) => {
            console.error(err);
            this.submitError = 'Failed to submit application.';
            this.isSubmitting = false;
          },
        });
      },
      error: () => {
        this.submitError = 'Failed to load user profile.';
        this.isSubmitting = false;
      },
    });
  }

  onCancel(): void {
    this.router.navigateByUrl('/dashboard/my-projects');
  }
}
