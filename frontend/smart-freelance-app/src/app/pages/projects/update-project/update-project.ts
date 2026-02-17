import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectService } from '../../../core/services/project.service';
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
      private router: Router){}
  
    ngOnInit(){
      this.id = Number(this.activatedRoute.snapshot.paramMap.get('id'));

      this.updateProjectForm = this.fb.group({
        id: this.id,
        title: [null, [Validators.required]],
        description: [null, [Validators.required]],
        budget: [null, [Validators.required]],
        deadline: [null, [Validators.required]],
        status: [null, [Validators.required]],
        category: ['', [Validators.required]],
        skillsRequiered: [null, [Validators.required]]
      })
      this.getProjectById();
    }
  
    getProjectById() {
      this.isLoading = true;
      this.ps.getProjectById(this.id).subscribe({
        next: (res) => {
          console.log(res);
          const formattedDate = res.deadline ? res.deadline.split('T')[0] : '';
          this.updateProjectForm.patchValue({
            ...res,
            deadline: formattedDate
          });
          this.isLoading = false;
        },
        error: (err) => {
          this.submitError = 'Failed to load project';
          this.isLoading = false;
        }
      });
    }
  
    updateProject() {
      if (this.updateProjectForm.invalid) return;

      this.isSubmitting = true;
      this.submitError = null;

      const formValue = { ...this.updateProjectForm.value };

      // ✅ SAME FIX AS ADD COMPONENT
      if (formValue.deadline && !formValue.deadline.includes('T')) {
        formValue.deadline = `${formValue.deadline}T00:00:00`;
      }

      this.ps.updateProject(formValue).subscribe({
        next: (res) => {
          console.log(res);
          this.submitSuccess = true;
          this.isSubmitting = false;

          setTimeout(() => {
            this.router.navigateByUrl("dashboard/my-projects");
          }, 1500);
        },
        error: (err) => {
          this.submitError = 'Failed to update project';
          this.isSubmitting = false;
        }
      });
    }
}
