import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectApplicationService } from '../../../core/services/project-application.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-update-application',
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './update-application.html',
  styleUrl: './update-application.scss',
})
export class UpdateApplication implements OnInit{
  id!: number;

  isLoading = false;
  isSubmitting = false;
  submitSuccess = false;
  submitError: string | null = null;

  updateApplicationForm!: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private applicationService: ProjectApplicationService,
  ) {}

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));

    this.updateApplicationForm = this.fb.group({
      id: this.id,
      projectId: [null],
      freelanceId: [null],
      proposedPrice: [null, [Validators.required, Validators.min(1)]],
      proposedDuration: [null, [Validators.required, Validators.min(1)]],
      coverLetter: ['', [Validators.required, Validators.minLength(20)]],
      status: ['PENDING'],
    });

    this.getApplicationById();
  }

  getApplicationById(): void {
    this.isLoading = true;

    this.applicationService.getApplicationById(this.id).subscribe({
      next: (res: any) => {
        if (res) {
          this.updateApplicationForm.patchValue({
            ...res,
            proposedPrice: res.proposedPrice ?? null,
            proposedDuration: res.proposedDuration ?? null,
            coverLetter: res.coverLetter ?? '',
          });
        }
        this.isLoading = false;
      },
      error: () => {
        this.submitError = 'Failed to load application';
        this.isLoading = false;
      },
    });
  }

  updateApplication(): void {
    if (this.updateApplicationForm.invalid) {
      this.updateApplicationForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.submitError = null;

    const payload = { ...this.updateApplicationForm.value };

    this.applicationService.updateApplication(payload).subscribe({
      next: () => {
        this.submitSuccess = true;
        this.isSubmitting = false;

        setTimeout(() => {
          this.router.navigateByUrl('/dashboard/my-applications');
        }, 1500);
      },
      error: () => {
        this.submitError = 'Failed to update application';
        this.isSubmitting = false;
      },
    });
  }

  onCancel(): void {
    this.router.navigateByUrl('/dashboard/my-applications');
  }
}
