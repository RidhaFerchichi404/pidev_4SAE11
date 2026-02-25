import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProjectService, Project } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-add-project',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-project.html',
  styleUrl: './add-project.scss',
})
export class AddProject implements OnInit {
  postProjectForm!: FormGroup;
  isSubmitting = false;
  submitSuccess = false;
  submitError: string | null = null;
  minDate!: string;

  constructor(
    private projectService: ProjectService,
    private authService: AuthService,
    private userService: UserService,
    private fb: FormBuilder,
    private router: Router
  ) {}

  ngOnInit() {

    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    this.minDate = `${yyyy}-${mm}-${dd}`;


    this.postProjectForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      budget: [null, [Validators.required, Validators.min(1)]],
      deadline: ['', [Validators.required, this.futureDateValidator.bind(this)]],
      status: ['', Validators.required],
      category: ['', Validators.required],
      skillsRequiered: ['', Validators.required]   // ← keep field name consistent with your backend
    });
  }

  futureDateValidator(control: any) {
    if (!control.value) return null; // empty handled by required

    const today = new Date();
    today.setHours(0,0,0,0); // start of today
    const inputDate = new Date(control.value);

    if (inputDate <= today) {
      return { invalidDate: true }; // validation error
    }
    return null;
  }

  postProject() {
    if (this.postProjectForm.invalid) {
      this.postProjectForm.markAllAsTouched();
      return;
    }


    
    const formValue = { ...this.postProjectForm.value };

    const skillsTrimmed = (formValue.skillsRequiered || '').trim();

    if (skillsTrimmed.length < 3) {  // or < 1 if you allow one short skill
      this.submitError = 'Please enter at least one valid skill (min 3 characters)';
      this.postProjectForm.get('skillsRequiered')?.setErrors({ minlength: true });
      this.isSubmitting = false;
      return;
    }

    // Fix deadline: turn "2025-03-08" → "2025-03-08T00:00:00" (or with your timezone if needed)
    if (formValue.deadline) {
      formValue.deadline = `${formValue.deadline}T00:00:00`;   // midnight
      // Alternative (full ISO with Z): formValue.deadline += 'T00:00:00Z';
    }

    this.isSubmitting = true;

    const email = this.authService.getPreferredUsername();
    if (!email) {
      this.submitError = 'You must be signed in to create a project.';
      this.isSubmitting = false;
      return;
    }

    this.userService.getByEmail(email).subscribe({
      next: (user) => {
        if (!user?.id) {
          this.submitError = 'Could not identify your account. Please try again.';
          this.isSubmitting = false;
          return;
        }
        // clientId = user id of the user who created the project (the logged-in CLIENT)
        const projectPayload = { ...formValue, clientId: user.id };
        this.projectService.createProject(projectPayload).subscribe({
          next: (res: Project | null) => {
            this.submitSuccess = true;
            this.isSubmitting = false;
            setTimeout(() => {
              this.router.navigateByUrl('/dashboard/my-projects');
            }, 1800);
          },
          error: (err: { error?: { message?: string } }) => {
            console.error('Create failed', err);
            this.submitError = err?.error?.message || 'Failed to create project. Please try again.';
            this.isSubmitting = false;
          },
        });
      },
      error: () => {
        this.submitError = 'Could not load your profile. Please try again.';
        this.isSubmitting = false;
      },
    });
  }

  onCancel() {
    this.router.navigateByUrl('/dashboard/my-projects');
  }

  // Optional helper – not needed anymore but you can keep it
  clearFormProject() {
    this.postProjectForm.reset();
  }
}
