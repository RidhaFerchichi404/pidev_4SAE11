import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectService, Project } from '../../../core/services/project.service';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-update-project',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './update-project.html',
  styleUrl: './update-project.scss',
})
export class UpdateProject {
  id!: number;
  isLoading = false;
  isSubmitting = false;
  submitSuccess = false;
  submitError: string | null = null;

  updateProjectForm!: FormGroup;

  constructor(
    private activatedRoute: ActivatedRoute,
    private ps: ProjectService,
    private fb: FormBuilder,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.id = Number(this.activatedRoute.snapshot.paramMap.get('id'));

    this.updateProjectForm = this.fb.group({
      id: this.id,
      clientId: [null as number | null],
      title: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      budget: [null, [Validators.required, Validators.min(1)]],
      deadline: ['', [Validators.required]],
      status: ['', [Validators.required]],
      category: ['', [Validators.required]],
      skillsRequiered: ['', [Validators.required, Validators.minLength(2)]]
    });
    this.getProjectById();
  }
  
  getProjectById(): void {
    this.isLoading = true;
    this.ps.getProjectById(this.id).subscribe({
      next: (res: Project | null) => {
        if (res) {
          const formattedDate = res.deadline ? String(res.deadline).split('T')[0] : '';
          this.updateProjectForm.patchValue({
            ...res,
            title: res.title ?? '',
            description: res.description ?? '',
            category: res.category ?? '',
            status: res.status ?? '',
            skillsRequiered: Array.isArray(res.skillsRequiered)
              ? (res.skillsRequiered as string[]).join(', ')
              : (res.skillsRequiered ?? ''),
            deadline: formattedDate
          });
        }
        this.isLoading = false;
      },
      error: () => {
        this.submitError = 'Failed to load project';
        this.isLoading = false;
      }
    });
  }

  updateProject(): void {
    if (this.updateProjectForm.invalid) {
      this.updateProjectForm.markAllAsTouched();
      return;
    }

      this.isSubmitting = true;
      this.submitError = null;

    const formValue = { ...this.updateProjectForm.value };

    if (formValue.deadline && !formValue.deadline.includes('T')) {
      formValue.deadline = `${formValue.deadline}T00:00:00`;
    }

    const skillsTrimmed = (formValue.skillsRequiered || '').trim();
    if (skillsTrimmed.length < 2) {
      this.updateProjectForm.get('skillsRequiered')?.setErrors({ minlength: true });
      this.isSubmitting = false;
      return;
    }

    this.ps.updateProject(formValue).subscribe({
      next: () => {
        this.submitSuccess = true;
        this.isSubmitting = false;
        setTimeout(() => {
          this.router.navigateByUrl('dashboard/my-projects');
        }, 1500);
      },
      error: () => {
        this.submitError = 'Failed to update project';
        this.isSubmitting = false;
      }
    });
  }
}
